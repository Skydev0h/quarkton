package app.quarkton.ton

import android.content.Context
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import app.quarkton.BuildConfig
import app.quarkton.Persistence
import app.quarkton.R
import app.quarkton.db.AppDatabase
import app.quarkton.db.DAppItem
import app.quarkton.db.NameItem
import app.quarkton.db.RateItem
import app.quarkton.db.TransItem
import app.quarkton.db.WalletItem
import app.quarkton.extensions.joinAllIgnoringAll
import app.quarkton.extensions.toRaw
import app.quarkton.extensions.toRepr
import app.quarkton.extensions.toWc
import app.quarkton.extensions.vrStr
import app.quarkton.ton.ext.nacl
import app.quarkton.ton.extensions.comment
import app.quarkton.ton.extensions.fromRepr
import app.quarkton.ton.extensions.toKey
import app.quarkton.ton.extensions.toRepr
import app.quarkton.utils.ConnectionState
import app.quarkton.utils.currentConnectivityState
import io.ktor.util.encodeBase64
import io.ktor.utils.io.core.toByteArray
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kotlincrypto.endians.BigEndian.Companion.toBigEndian
import org.kotlincrypto.endians.LittleEndian.Companion.toLittleEndian
import org.ton.api.liteclient.config.LiteClientConfigGlobal
import org.ton.api.validator.config.ValidatorConfigGlobal
import org.ton.bigint.BigInt
import org.ton.block.AddrStd
import org.ton.block.Coins
import org.ton.block.DnsNextResolver
import org.ton.block.DnsRecord
import org.ton.block.DnsSmcAddress
import org.ton.block.StateInit
import org.ton.block.VmStackCell
import org.ton.block.VmStackValue
import org.ton.boc.BagOfCells
import org.ton.cell.Cell
import org.ton.cell.CellBuilder
import org.ton.contract.wallet.MessageData
import org.ton.contract.wallet.WalletTransfer
import org.ton.crypto.digest.sha256
import org.ton.crypto.encodeHex
import org.ton.crypto.encoding.base64
import org.ton.crypto.hex
import org.ton.lite.api.liteserver.LiteServerAccountId
import org.ton.lite.client.LiteClient
import org.ton.lite.client.internal.TransactionId
import org.ton.mnemonic.Mnemonic
import org.ton.tlb.CellRef
import org.ton.tlb.loadTlb
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max

class DataMaster(
    val ctx: Context,
    val db: AppDatabase,
    val per: Persistence
) {

    private val http = OkHttpClient.Builder().build()

    val json = Json{ ignoreUnknownKeys = true }
    val crs = CoroutineScope(Dispatchers.IO)
    val lso = mutableListOf<LiteClientConfigGlobal>()

    /*
    val mtx = mutableMapOf<MXType, Mutex>()
    val mst = mutableMapOf<MXType, MutableState<Boolean>>()
    val acmtx = Mutex()
    */

    val refreshing = atomic(0)
    val otherRefreshing = atomic(0)
    val sending = atomic(0)
    val loadingMore = atomic(0)

    val isConnected = mutableStateOf(false)
    val isRefreshing = mutableStateOf(false)
    val isLoadingMore = mutableStateOf(false)
    val doneServersLM = mutableStateOf(0)

    val isSending = mutableStateOf(false)

    val refreshingOtherSince = mutableStateOf(0L)
    val isRefreshingOther = mutableStateOf(false)
    val otherRefrProg = mutableStateOf("")

    val tcl = TCLink(http.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.MINUTES)
        .writeTimeout(1, TimeUnit.MINUTES)
        .build(), db, json, crs, ::tcUpperHandleEvent)

    val tcRequestedConnect = mutableStateOf(false)
    val tcIsConnected = derivedStateOf { tcl.isConnected.value }
    val tcIsConnecting = derivedStateOf { tcl.isConnecting.value || tcRequestedConnect.value }
    val tcCurrentApp = mutableStateOf<String?>(null) // TODO

    val tcWantConnected = mutableStateOf(false)
    var tcd: TCData? = null

    val tcIsPendingSend = mutableStateOf(false)
    var tcPendingSend: Triple<Long, List<WalletTransfer>, Long?>? = null

    var walletRefreshInterval = 60

    // enum class MXType { MX_REFRESH }

    companion object {
        const val LOG = "DataMaster"
        const val GLOBAL_CONFIG_URL = "https://ton.org/global-config.json"
        const val GLOBAL_CONFIG_FILE = "global-config.json"
        const val DNS_ROOT_ADDR = "E56754F83426F69B09267BD876AC97C44821345B7E266BD956A7BFBFB98DF35C"
        const val TC_DEFAULT_BRIDGE = "tonapi"
        const val FALLBACK_BRIDGE_URL = "https://bridge.tonapi.io/bridge/"

        val bridges = mapOf(
            "tonapi" to Pair("https://bridge.tonapi.io/bridge/", "TON API Bridge"),
            "whales" to Pair("https://connect.tonhubapi.com/tonconnect/", "Whales Connect")
        )

        val currencies = mapOf(
            "USD" to Pair("$ ~",   "United States Dollar"),
            "EUR" to Pair("€ ~",   "Euro"),
            "UAH" to Pair("~ ₴",   "Ukrainian hryvnia"),
            "AED" to Pair("~ DH",  "United Arab Emirates Dirham"),
            "RUB" to Pair("~ ₽",   "Russian Ruble"),
            "UZS" to Pair("~ Sum", "Uzbekistani sum"),
            "GBP" to Pair("£ ~",   "Great Britain Pound"),
            "CHF" to Pair("₣ ~",   "Swiss Franc"),
            "CNY" to Pair("¥ ~",   "China Yuan"),
            "KRW" to Pair("₩ ~",  "South Korean Won"),
            "IDR" to Pair("~ Rp",  "Indonesian Rupiah"),
            "INR" to Pair("₹ ~",   "Indian Rupee"),
            "JPY" to Pair("¥ ~",   "Japanese Yen")
        )

        private const val DNS_NAME_DELIMITER = 0.toChar().toString()

        fun encodeHostname(host: String?): String {
            if (host.isNullOrEmpty() || host == ".") {
                return DNS_NAME_DELIMITER
            }
            val delimiter = 0.toChar().toString()
            return host.lowercase().split('.').asReversed().joinToString(DNS_NAME_DELIMITER, postfix = delimiter)
        }

        fun decodeHostname(host: String): String {
            return host.split(DNS_NAME_DELIMITER).asReversed().joinToString(".")
        }
    }

    fun clearDatabase() {
        crs.launch {
            db.walletDao().deleteAll()
            db.transDao().deleteAll()
            db.nameDao().deleteAll()
        }
    }

    //********************************************************************************************//
    //
    //                                   CURRENCIES UPDATER
    //
    //********************************************************************************************//

    // N.B. It is IMPOSSIBLE to obtain currencies for that little small gray text without querying
    // third-party API such as TON API. As of now no reliable oracles exist in TON that could give
    // that information to blockchain.

    fun updateCurrenciesInBackground() {
        crs.launch {
            try {
                val minUpdated = db.rateDao().getMinUpdated() ?: 0L
                if (nowms() - minUpdated <= 300_000L) // 5 minutes
                    return@launch
                val req = Request.Builder()
                    .url(
                        "https://tonapi.io/v2/rates?tokens=ton&currencies=" +
                                currencies.keys.joinToString("%2C")
                    )
                    .build()
                Log.i("DataMaster", "Requesting currencies update")
                val str = http.newCall(req).execute().use { r ->
                    if (!r.isSuccessful || r.body == null) {
                        Log.w("DataMaster", "HTTP Request failed")
                        return@use null
                    }
                    return@use r.body!!.string()
                } ?: return@launch
                Log.i("DataMaster", "Updating currencies: $str")
                val json = json.parseToJsonElement(str)
                db.rateDao().setList(
                    json.jsonObject["rates"]?.jsonObject?.get("TON")?.jsonObject?.get("prices")
                        ?.jsonObject?.map {
                            RateItem(it.key,
                                nowms(),
                                (it.value as? JsonPrimitive)?.doubleOrNull ?: 0.0,
                                currencies.keys.indexOf(it.key).let { i -> if (i < 0) 999 else i })
                        } ?: listOf()
                )
            } catch (e: Throwable) {
                Log.e("DataMaster", "Failed to update currencies in background")
            }
        }
    }

    //********************************************************************************************//
    //
    //                                 GLOBAL CONFIG OPERATIONS
    //
    //********************************************************************************************//

    fun updateGlobalConfigNow(url: String = GLOBAL_CONFIG_URL) {
        val req = Request.Builder().url(url).build()
        Log.i("DataMaster", "Requesting global config update")
        val str = http.newCall(req).execute().use { r ->
            if (!r.isSuccessful || r.body == null) {
                Log.w("DataMaster", "HTTP Request failed")
                return@use null
            }
            return@use r.body!!.string()
        } ?: return
        Log.i(LOG, "Loaded global-config from $url")
        val conf = json.decodeFromString<LiteClientConfigGlobal>(str)
        val emvc = ctx.resources.openRawResource(R.raw.zerostate).use {
            json.decodeFromString<ValidatorConfigGlobal>(it.bufferedReader().readText())
        }
        // initBlock and zeroState MUST match verbatim!
        if (emvc.initBlock != conf.validator.initBlock)
            throw SecurityException("initBlock of ton global-config does not match embedded one")
        if (emvc.zeroState != conf.validator.zeroState)
            throw SecurityException("zeroState of ton global-config does not match embedded one")
        // hardforks MUST at least contain the embedded ones!
        emvc.hardforks.forEach {
            if (!conf.validator.hardforks.contains(it))
                throw SecurityException("hardforks of ton global-config does not contain embedded one at ${it.seqno}")
        }
        // hardforks MAY NOT contain any differing entries earlier than the last embedded one!
        val hiseqno = emvc.hardforks.map { it.seqno }.reduce{ x, y -> max(x, y) }
        conf.validator.hardforks.forEach {
            if ((it.seqno <= hiseqno) and !emvc.hardforks.contains(it))
                throw SecurityException("hardforks of ton global-config contain ${it.seqno} hardfork not from embedded")
        }
        val file = File(ctx.filesDir, GLOBAL_CONFIG_FILE)
        file.writeText(str)
        Log.i(LOG, "Updated cached global-config")
    }

    fun updateGlobalConfig(recheckInterval: Int = 3600, url: String = GLOBAL_CONFIG_URL) {
        val file = File(ctx.filesDir, GLOBAL_CONFIG_FILE)
        if (file.exists()) {
            val diff = (nowms() - file.lastModified()) / 1000
            if (diff < recheckInterval) {
                Log.i(LOG, "global-config is too fresh ($diff/$recheckInterval), update skipped")
                return
            }
        }
        updateGlobalConfigNow(url)
    }

    fun updateGlobalConfigInBackground(recheckInterval: Int = 3600, url: String = GLOBAL_CONFIG_URL) {
        crs.launch {
            try {
                Log.i(LOG, "Requested background global-config update")
                updateGlobalConfig(recheckInterval, url)
            } catch (e: Throwable) {
                Log.e(LOG, "Failed to update global config in background", e)
            }
        }
    }

    fun ensureGlobalConfig(recheckInterval: Int = 3600, url: String = GLOBAL_CONFIG_URL): String {
        val f = File(ctx.filesDir, GLOBAL_CONFIG_FILE)
        @Suppress("LiftReturnOrAssignment")
        if (f.exists()) {
            val res = f.readText()
            updateGlobalConfigInBackground(recheckInterval, url)
            return res
        }
        else {
            updateGlobalConfigNow(url)
            return File(ctx.filesDir, GLOBAL_CONFIG_FILE).readText()
        }
    }

    fun globalConfig(): LiteClientConfigGlobal = parseGlobalConfig(ensureGlobalConfig())

    fun parseGlobalConfig(s: String): LiteClientConfigGlobal =
        json.decodeFromString<LiteClientConfigGlobal>(s)

    //********************************************************************************************//
    //
    //                             LITE SERVER LIST OPERATIONS
    //
    //********************************************************************************************//

    fun shuffleLiteServers(conf: LiteClientConfigGlobal): LiteClientConfigGlobal =
        conf.copy(liteServers = conf.liteServers.shuffled())

    fun reorderLiteServers(conf: LiteClientConfigGlobal, i: Int, n: Int): LiteClientConfigGlobal {
        // Maintain a constant reorder for each LiteClient thread
        // Under the hood LiteClient maintains pool of connections with 10s timeout
        // It also may fail over to next one in list if current server times out request
        // Therefore, each thread should have its own shuffle of lite servers that does not change
        // BUT! To prevent overloading some servers by lots of clients each client should generate
        //      its own shuffle, but only once per application start OR device
        // Using multiple (like 3) threads will help to mitigate risks of getting broken LS first
        while ((lso.size < n) or (lso.size < i + 1))
            lso.add(shuffleLiteServers(conf))
        return lso[i]
    }

    fun reshakeLiteServers(i: Int) {
        if (i < lso.size) {
            lso[i] = shuffleLiteServers(globalConfig())
        }
    }

    //********************************************************************************************//
    //
    //                                   OPERATION WRAPPER
    //
    //********************************************************************************************//

    fun noConnection(): Boolean = ctx.currentConnectivityState === ConnectionState.Unavailable

    fun multipass(multiCnt: Int = 3, multiOffset: Int = 0,
                  crs: CoroutineScope? = null, onDone: ((n: Int, i: Int) -> Unit)? = null,
                  action: suspend (lc: LiteClient, i: Int, n: Int) -> Boolean): List<Job> {
        val cs = crs ?: CoroutineScope(Dispatchers.IO)
        val conf = globalConfig()
        @Suppress("LocalVariableName")
        val i_am = atomic(0) // !!!
        return (0 until multiCnt).map { ii ->
            val i = ii + multiOffset
            cs.launch {
                try {
                    val lc = LiteClient(this.coroutineContext, reorderLiteServers(conf, i, multiCnt))
                    if (action(lc, i, multiCnt)) // True to end all others, False to let them work
                        cs.cancel("$i is done")
                    // Using False (not cancelling) here may be useful for sending messages
                    Log.i(LOG, "$i is done")
                    val locdone = i_am.incrementAndGet()
                    onDone?.invoke(locdone, i)
                } catch (e: Throwable) {
                    if ((e is CancellationException) and (e.message?.endsWith(" is done") == true))
                        Log.i(LOG, "$i killed: ${e.message}")
                    else if ((e.cause is CancellationException) and (e.cause?.message?.endsWith(" is done") == true))
                        Log.i(LOG, "$i killed: ${e.cause?.message} (causing ${e.message})")
                    else if ((e is NullPointerException) and (e.message?.contains("liteClientConfigGlobal") == true)) {
                        Log.e(LOG, "$i tried to crash: ${e.message}")
                        Log.i(LOG, "supplied config: " + reorderLiteServers(conf, i, multiCnt).toString())
                        try { reshakeLiteServers(i) } catch (_: Throwable) {}
                    }
                    else { // DO NOT throw exceptions up! It will crash the application!
                        Log.e(LOG, "$i lite client failed", e)
                        try { reshakeLiteServers(i) } catch (_: Throwable) {}
                    }
                }
            }
        }
    }

    //********************************************************************************************//
    //
    //                                   CURRENT WALLET OPERATIONS
    //
    //********************************************************************************************//

    fun getCurrentVerRev() = db.walletDao().getCurrent()?.verRev ?: Wallet.V4R2

    fun getWallet(verRev: Int? = null): Wallet? {
        val vr = verRev ?: getCurrentVerRev()
        val sf = per.getSeedPhrase()
        if (sf == null) {
            Log.w(LOG, "No seed phrase found")
            return null
        }
        return Wallet(abs(vr), Mnemonic.toKey(sf), vr.toWc())
    }

    fun refreshCurrentWallet(multi: Int = 3) {
        if (noConnection()) return
        if (isRefreshing.value) return
        if (!refreshing.compareAndSet(0, 1)) return
        try {
            isRefreshing.value = true
            isConnected.value = false
            crs.launch {
                try {
                    val vr = getCurrentVerRev()
                    val wal = getWallet(vr) ?: return@launch
                    val best = atomic(-1)
                    Log.i(LOG, "Refreshing current wallet (${vr.toString(16)})")
                    val mx = Mutex(true)
                    multipass(multi, onDone = { n, i ->
                        if (n == 1) {
                            best.compareAndSet(-1, i)
                            try { mx.unlock() } catch (_: Throwable) {}
                        }
                    }) { lc, _, _ ->
                        isConnected.value = true
                        wal.update(lc)
                        db.walletDao().set(WalletItem.fromWallet(wal, true))
                        return@multipass true
                    } // .joinAllIgnoringAll()
                    mx.lock()
                    Log.i(LOG, "Fastest server: ${best.value}")
                    val ltx = db.transDao().getLastByAccount(wal.address.toRepr())
                    val hasTx = wal.data.value.fullState?.lastTransactionId != null
                    if (hasTx and ((ltx == null) or (ltx?.id != wal.data.value.fullState?.lastTransactionId?.toRepr()))) {
                        Log.i(LOG, "Refreshing transactions on the wallet (initial)")
                        // Need to try to get transactions from more different servers because they remember different amount of data
                        // But that takes too much time, so true here
                        val topKnownLT = ltx?.lt ?: 0L
                        var leastNewLT = Long.MAX_VALUE
                        var nextRequest: TransactionId? = null
                        val atomicMX = Mutex()
                        Log.i(LOG, "Top Known LT: $topKnownLT")
                        multipass(multi) { lc, i, _ ->
                            val txs = wal.getTransactions(lc)
                            txs.forEach {
                                db.transDao().set(TransItem.fromTransactionInfo(it, wal.workchainId))
                                atomicMX.withLock {
                                    if (it.id.lt < leastNewLT) {
                                        leastNewLT = it.id.lt
                                        nextRequest = it.id /* TransactionId(
                                            it.transaction.value.prevTransHash,
                                            it.transaction.value.prevTransLt.toLong()) */
                                    }
                                }
                            }
                            if (txs.isEmpty()) {
                                // It seems that this lite server does not have transactions for
                                //    this wallet. Try reshaking this LS list on this index
                                reshakeLiteServers(i)
                            } else
                                try { mx.unlock() } catch (_: Throwable) {}
                            return@multipass txs.isNotEmpty() // If not found, do not interrupt others
                        } // .joinAllIgnoringAll()
                        mx.lock()
                        if (leastNewLT == Long.MAX_VALUE) leastNewLT = 0L
                        Log.i(LOG, "Least New LT: $leastNewLT")
                        // Detect and close gaps with transactions
                        // That is, keep querying new transactions until result is empty
                        //      OR the smallest incoming LT is LESS than HIGHEST known entry LT
                        var iters = 100
                        while (leastNewLT > topKnownLT) {
                            if (iters-- == 0) break // Runaway guard
                            val captured = nextRequest
                            val prevLeastNewLT = leastNewLT
                            leastNewLT = Long.MAX_VALUE
                            nextRequest = null
                            if (captured != null) {
                                Log.i(LOG, "Continue transactions dl from ${captured.toRepr()}")
                                multipass(multi) { lc, _, _ ->
                                    wal.getTransactions(lc).forEach {
                                        db.transDao().set(TransItem.fromTransactionInfo(it, wal.workchainId))
                                        atomicMX.withLock {
                                            if (it.id.lt < leastNewLT) {
                                                leastNewLT = it.id.lt
                                                nextRequest = it.id /*TransactionId(
                                                    it.transaction.value.prevTransHash,
                                                    it.transaction.value.prevTransLt.toLong())*/
                                            }
                                        }
                                    }
                                    try { mx.unlock() } catch (_: Throwable) {}
                                    return@multipass true
                                } // .joinAllIgnoringAll()
                                mx.lock()
                            }
                            Log.i(LOG, "Least New LT: $leastNewLT (Top Known LT: $topKnownLT)")
                            if (prevLeastNewLT == leastNewLT) break
                            if (leastNewLT == Long.MAX_VALUE) leastNewLT = 0L
                        }
                        try { mx.unlock() } catch (_: Throwable) {}
                    } else {
                        Log.i(LOG, "Transactions refresh not needed")
                    }
                } finally {
                    if (walletRefreshInterval < 60)
                        walletRefreshInterval = 60
                    isRefreshing.value = false
                }
            }
        } finally {
            refreshing.value = 0
        }
    }

    fun loadMoreTransactions(multi: Int = 3) {
        if (noConnection()) return
        if (isLoadingMore.value) return
        if (!loadingMore.compareAndSet(0, 1)) return
        try {
            doneServersLM.value = 0
            isLoadingMore.value = true
            crs.launch {
                var anyFound = false
                try {
                    val addr = db.walletDao().getCurrent()?.address ?: return@launch
                    val txid = db.transDao().getFirstByAccount(addr)?.prevId ?: return@launch
                    Log.i(LOG, "Loading more transactions from $txid for $addr")
                    val wal = getWallet() ?: return@launch
                    val mx = Mutex(true)
                    multipass(multi, onDone = { n, _ -> doneServersLM.value = n }) { lc, i, _ ->
                        val txs = wal.getTransactions(lc, after = TransactionId.fromRepr(txid))
                        txs.forEach {
                            db.transDao().set(TransItem.fromTransactionInfo(it, wal.workchainId))
                        }
                        if (txs.isEmpty()) {
                            Log.i(LOG, "$i not found any txs")
                            // It seems that this lite server does not have transactions for
                            //    this wallet. Try reshaking this LS list on this index
                            reshakeLiteServers(i)
                        } else {
                            Log.i(LOG, "$i found more txs (${txs.size}): ${txs.first().id.lt} - ${txs.last().id.lt}")
                            anyFound = true
                            try { mx.unlock() } catch (_: Throwable) {}
                        }
                        return@multipass txs.isNotEmpty() // If not found, do not interrupt others
                    } // .joinAllIgnoringAll()
                    mx.lock()
                    try { mx.unlock() } catch (_: Throwable) {}
                } finally {
                    if (!anyFound) {
                        doneServersLM.value = -1
                        delay(3000)
                    }
                    isLoadingMore.value = false
                }
            }
        } finally {
            loadingMore.value = 0
        }
    }

    fun maybeRefreshCurrentWallet(multi: Int = 3) {
        if (!per.isSetUp()) return
        if (refreshing.value != 0) return
        if (noConnection()) return
        crs.launch {
            val cw = db.walletDao().getCurrent()
            if ((cw != null) && (nowms() - cw.updated < walletRefreshInterval * 1000))
                return@launch
            refreshCurrentWallet(multi)
        }
    }

    //********************************************************************************************//
    //
    //                                    REFRESH OTHER WALLETS
    //
    //********************************************************************************************//

    fun refreshOtherWallets(multi: Int = 5, workchainId: Int = 0, offset: Int = 0) {
        if (noConnection()) return
        if (isRefreshingOther.value) return
        if (!otherRefreshing.compareAndSet(0, 1)) return
        try {
            isRefreshingOther.value = true
            refreshingOtherSince.value = nowms()
            otherRefrProg.value = ctx.getString(R.string.starting)
            val sf = per.getSeedPhrase()
            if (sf == null) {
                Log.w(LOG, "No seed phrase found")
                return
            }
            crs.launch {
                try {
                    // val jobs = mutableListOf<List<Job>>()
                    val mxs = mutableListOf<Mutex>()
                    Wallet.useWallets.forEach {
                        val mx = Mutex(true)
                        mxs.add(mx)
                        otherRefrProg.value = "${it.vrStr()}..." + (if (workchainId == -1) " (MC)" else "")
                        val wal = Wallet(abs(it), Mnemonic.toKey(sf), workchainId)
                        Log.i(LOG, "refreshOtherWallets starting for ${it.toString(16)}")
                        // jobs.add(
                        multipass(multi, offset) { lc, _, _ ->
                            isConnected.value = true
                            wal.update(lc)
                            val cw = db.walletDao().getCurrent()
                            db.walletDao().set(
                                WalletItem.fromWallet(
                                    wal,
                                    (if (workchainId < 0) -it else it) == cw?.verRev
                                )
                            )
                            try { mx.unlock() } catch (_: Throwable) {}
                            return@multipass true
                        }
                        // )
                    }
                    // jobs.forEach { it.joinAllIgnoringAll() }
                    mxs.forEach {
                        it.lock()
                        try { it.unlock() } catch (_: Throwable) {}
                    }
                } finally {
                    otherRefrProg.value = ""
                    isRefreshingOther.value = false
                }
            }
        } finally {
            otherRefreshing.value = 0
        }
    }

    //********************************************************************************************//
    //
    //                                    PERIODICAL REFRESHER
    //
    //********************************************************************************************//

    suspend fun periodicalRefresher(interval: Int = 5) {
        while (true) {
            delay(interval * 1000L)
            Log.d(LOG, "Periodical refresh execution")
            maybeRefreshCurrentWallet()
            updateCurrenciesInBackground()
        }
    }

    //********************************************************************************************//
    //
    //                                            TON DNS
    //
    //********************************************************************************************//

    suspend fun dnsLookup(dom: String, multi: Int = 11, quorum: Int = 5): String? {
        // WHATEVER: defer until other core functionality is completed
        // First making dnsLookup as of now with quorum system
        // Might be very difficult to check proofs for remote SMC execution
        // Otherwise HUGE state of .ton DNS will need to be downloaded
        // It will be needed to download it later anyway for reverse resolution
        // TODO: really check the proofs, then it will be possible to accelerate lookups by race alg
        val path = encodeHostname(dom)
        val mx = Mutex(true)
        val results = mutableListOf<String?>()
        val roots = mutableListOf<String?>()
        var result: String? = null
        var resultDone = false
        var rootsDone = false
        val cachedDnsRoot = per.cachedDNSRoot
        if (cachedDnsRoot != "") {
            val (_, cachedDnsRootSince) = cachedDnsRoot.split("@", limit = 2)
            if (nowms() - cachedDnsRootSince.toLong() < 86400_000L)
                rootsDone = true
        }
        fun genericMajority(from: Collection<String?>, amt: Int, what: (String?) -> Unit): Boolean {
            val keys = from.groupingBy{ it }.eachCount()
                .filter{ it.value >= amt }.keys
            if (keys.isNotEmpty()) {
                what(keys.first())
                return true
            }
            return false
        }
        fun checkMajority(): Boolean {
            return genericMajority(results, quorum) {
                if (resultDone) return@genericMajority
                resultDone = true
                result = it
                Log.i("DataMaster", "DNS Majority result: $result")
                try { mx.unlock() } catch (_: Throwable) {}
            }
        }
        fun checkRootsMajority(): Boolean {

            return genericMajority(roots, quorum) {
                if (it == null) return@genericMajority
                if (rootsDone) return@genericMajority
                rootsDone = true
                per.cachedDNSRoot = "$it@${nowms()}"
                Log.i("DataMaster", "DNS Majority cached DNS root: $it")
            }
        }
        val category = BigInt(sha256("wallet".toByteArray()).encodeHex(), 16)
        multipass(multi) { lc, i, n ->
            val root = dnsGetRootAddress(lc)
            Log.i("DataMaster", "DNS Root (Config 4): " + root.id.encodeHex())
            roots.add(root.id.encodeHex())
            checkRootsMajority()
            Log.i("DataMaster", "DNS Lookup: " + path.replace(0.toChar(), '.'))
            var curPath = path
            var resolver = root
            while (true) {
                val sr = dnsResolutionStep(lc, resolver, curPath, category)
                Log.i("DataMaster", "Step: ${sr.first?.replace(0.toChar(), '.')} | ${sr.second?.workchain}:${sr.second?.id}")
                if (sr.second == null) { // No resolver, this is the end result
                    synchronized(results) {
                        Log.i("DataMaster", "DNS Resolve $i/$n result: ${sr.first}")
                        results.add(sr.first)
                        return@multipass checkMajority()
                    }
                }
                if (sr.first == null) { // Should never happen. Not registering this result.
                    return@multipass false
                }
                curPath = sr.first!!
                resolver = sr.second!!
            }
            @Suppress("UNREACHABLE_CODE")
            return@multipass true
        }
        mx.lock()
        val r = result
        if (r != null)
            db.nameDao().set(NameItem(dom, r, nowms()))
        return r
    }

    @Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
    private suspend fun dnsGetRootAddress(lc: LiteClient): LiteServerAccountId {
        val cache = per.cachedDNSRoot
        if (cache != "") {
            val (addr, since) = cache.split("@", limit = 2)
            if (nowms() - since.toLong() < 86400_000L)
                return LiteServerAccountId(-1, hex(addr))
        }
        return LiteServerAccountId(-1,
            hex("E56754F83426F69B09267BD876AC97C44821345B7E266BD956A7BFBFB98DF35C"))
        /*
        // FIXME: Does not work for some reason, does not "see" ANY config variable... empty hashmap
        val lastBlock = lc.getLastBlockId()
        // TODO: Load partial only config listOf(4)
        val configInfo = lc.liteApi.invoke(LiteServerGetConfigAll(0, lastBlock))
        var cell = BagOfCells(configInfo.configProof.toByteArray())
            .first().beginParse().loadRef().beginParse()
        cell.loadRefs(3) // skip pruned cells:
        // out_msg_queue_info accounts ^[overload_ ... Maybe BlkMasterInfo)]
        cell = cell.loadRef().beginParse() // load cell custom:(Maybe ^McStateExtra)
        cell.loadRef() // skip shard_hashes:ShardHashes
        val config = cell.loadRef().beginParse()
            .loadTlb(HashMapE.tlbCodec(32, Cell.tlbCodec())) // load config:^(Hashmap 32 ^Cell)
        val (_, rawAddress) = config.find { (key, _) ->
            Log.w("DataMaster", "Config found key ${key.toByteArray().encodeHex()}")
            CellSlice(key).loadUInt32().toInt() == 4
        } ?: throw IllegalStateException("No dns resolver address found")
        return LiteServerAccountId(-1,
            rawAddress.beginParse().loadBits(256).toByteArray())
        */
    }

    private suspend fun dnsResolutionStep(lc: LiteClient, resolver: LiteServerAccountId,
                                          remainingPath: String, category: BigInt): Pair<String?, LiteServerAccountId?> {
        try {
            val executionResult = lc.runSmcMethod(resolver, "dnsresolve",
                VmStackValue(CellBuilder.createCell { storeBytes(remainingPath.toByteArray()) }.beginParse()),
                VmStackValue(category)).toMutableVmStack()
            val n = (remainingPath.length * 8).toLong()
            Log.i("DataMaster", executionResult.stack.joinToString(",") { it.javaClass.simpleName })
            val m = executionResult.popTinyInt()
            if (m == 0L)
                return Pair(null, null) // Not found
            val v = executionResult.pop()
            if (v !is VmStackCell) {
                // Well, it can be only null
                return Pair(null, null) // Category not found
            }
            val c = v.cell
            Log.i("DataMaster", "$m | " + c.bits.toHex() + " " + c.refs.size)
            /*
            if (m == 8L && n == 8L) {
                // Final
                val dataCodec = HashMapE.tlbCodec(256, Cell.tlbCodec(DnsRecord))
                val all = CellBuilder.createCell { storeRef(c) }.beginParse().loadTlb(dataCodec)
                all.forEach {
                    Log.i("DataMaster", "Category ${it.first}: ${it.second}")
                }
                return Pair(null, null)
            }
            */
            val r = c.parse { loadTlb(DnsRecord) }
            if (m == n) {
                if (r !is DnsSmcAddress)
                    throw IllegalStateException("Resolution completed but result is not SMC Address")
                return Pair(r.smc_address.toRepr(), null)
            }
            if (r !is DnsNextResolver)
                throw IllegalStateException("Resolution not completed but result is not next resolver")
            return Pair(remainingPath.substring(m.toInt() / 8 + 1).ifEmpty { "\u0000" },
                with (r.resolver) { LiteServerAccountId(workchainId, address) })
        }
        catch (e: Throwable) {
            Log.e("DataMaster", "dnsResolutionStep failed for " +
                    "${remainingPath.replace(0.toChar(), '.')} " +
                    "(resolver: ${resolver.workchain}: ${resolver.id.encodeHex()})", e)
            return Pair(null, null)
        }
    }

    //********************************************************************************************//
    //
    //                                        SEND TRANSACTION
    //
    //********************************************************************************************//

    fun sendTransaction(address: String, amount: Long, comment: String, sendAll: Boolean, multi: Int = 5): Boolean {
        if (isSending.value) return false
        if (!sending.compareAndSet(0, 1)) return false
        try {
            isSending.value = true
            crs.launch {
                try {
                    val wal = getWallet() ?: return@launch
                    multipass(multi, onDone = { _, _ ->
                        // One is done, let's keep others sending in background
                        isSending.value = false
                        walletRefreshInterval = 5
                    }) { lc, _, _ ->
                        var halt = false
                        try {
                            wal.transfer(lc, WalletTransfer {
                                 destination = AddrStd(address)
                                if (sendAll)
                                    sendMode = 2 + 128
                                else
                                    coins = Coins.ofNano(amount)
                                if (comment != "")
                                    messageData = MessageData.comment(comment)
                                bounceable = false
                            })
                        } catch (e: Throwable) {
                            Log.e(LOG, "Send transaction error", e)
                            halt = true
                        }
                        return@multipass halt
                    }.joinAllIgnoringAll()
                    walletRefreshInterval = 5 // Refresh more often to faster find that TX
                } finally {
                    // In case of errors
                    isSending.value = false
                }
            }
        } finally {
            sending.value = 0
        }
        return true
    }

    //********************************************************************************************//
    //
    //                                        TON CONNECT
    //
    //********************************************************************************************//

    suspend fun tonConnectLooper() {
        while (true) {
            try {
                if (tcl.teardown) {
                    tcWantConnected.value = false
                    tcl.teardown = false
                }
                if (db.walletDao().getCurrentAddress() != db.dappDao().getActiveWallet()) {
                    Log.w("DataMaster", "Wallet change, wallet: ${db.walletDao().getCurrentAddress()}, dapp: ${db.dappDao().getActiveWallet()}")
                    tcDisconnect()
                    tcd = null
                    db.dappDao().setCurrent("")
                }
                if (tcd != null && tcd?.connect != tcWantConnected.value) {
                    tcd = tcd?.copy(connect = tcWantConnected.value)
                }
                tcl.new = tcd
                val lr = tcl.loop()
                tcCurrentApp.value = tcl.cur?.id
//                if (tcl.teardown) {
//                    db.dappDao().setCurrent("")
//                    tcl.new = null
//                    tcd = null
//                    tcl.teardown = false
//                }
                delay(1000L)
                tcRequestedConnect.value = false
                if (lr) delay(4000L)
            }
            catch (e: CancellationException) {
                tcl.new = null
                tcl.loop()
                throw e
            }
        }
    }

    fun tcConnect() {
        crs.launch {
            val item = db.dappDao().getActive()
            if (item == null) {
                tcd = null
                return@launch
            }
            tcWantConnected.value = true
            tcd = TCData.fromItem(item, per.tcBridge)
            tcRequestedConnect.value = true
        }
    }

    fun tcDisconnect() {
        tcWantConnected.value = false
        tcRequestedConnect.value = false
    }

    fun tcDeviceInfo(): JsonObject {
        return buildJsonObject {
            put("platform", "android")
            // put("appName", ctx.resources.getString(R.string.app_name))
            // FIXME: !!! Temporary !!! Wallet SDK DOES NOT work with wallet not in wallets-list
            // And the deadline is too close to wait for inclusion in that list to make it usable!
            put("appName", "Tonkeeper") // TODO: Include wallet in wallets-list and change this to real name
            put("appVersion", BuildConfig.VERSION_NAME)
            put("maxProtocolVersion", 2)
            put("features", buildJsonArray {
                add(buildJsonObject {
                    put("name", "SendTransaction")
                    put("maxMessages", 4)
                })
            })
        }
    }

    fun tcUpperHandleEvent(method: String, params: JsonElement?, id: Long): Pair<Int?, String> {
        if (method != "sendTransaction") {
            return 4041 to "Unknown method (Upper layer)"
        }
        val wal = getWallet() ?: return 5001 to "Invalid wallet state"
        val par = params!!.jsonArray[0].jsonPrimitive.content
        val jp = json.parseToJsonElement(par)
        val net = jp.jsonObject["network"]?.jsonPrimitive?.content
        if ((net != null) && (net != "-239")) return 4030 to "Invalid network"
        val from = jp.jsonObject["from"]?.jsonPrimitive?.content
        if ((from != null) && (from.lowercase() != wal.address.toRaw().lowercase())) {
            return 5030 to "Invalid wallet"
        }
        val valu = jp.jsonObject["valid_until"]?.jsonPrimitive?.longOrNull
        val msgs = jp.jsonObject["messages"]?.jsonArray ?: return 5031 to "No messages provided"
        if (msgs.size !in 1 .. 4) return 5032 to "Invalid messages count"
        val wts = msgs.map {
            try {
                WalletTransfer {
                    destination = AddrStd(it.jsonObject["address"]!!.jsonPrimitive.content)
                    coins = Coins.ofNano(it.jsonObject["amount"]!!.jsonPrimitive.long)
                    val body = it.jsonObject["payload"]?.jsonPrimitive?.content
                    val init = it.jsonObject["stateInit"]?.jsonPrimitive?.content
                    if (body != null || init != null) {
                        messageData = MessageData.raw(
                            if (body != null) BagOfCells(base64(body)).first() else Cell.empty(),
                            if (init != null) CellRef(BagOfCells(base64(init)).first(), StateInit.tlbCodec()) else null
                        )
                    }
                    bounceable = false
                }
            } catch (e: Throwable) {
                return 5033 to "Invalid messages content"
            }
        }
        val rectvalu = valu?.let { if (it > 1000000000000L) it/1000L else it }
        if ((rectvalu != null) && (now() > rectvalu))
            return 4100 to "Message already expired"
        tcPendingSend = Triple(id, wts, rectvalu)
        tcIsPendingSend.value = true
        tcWantConnected.value = false // Disconnect so that we do not receive further events until resolution
        return -1 to ""
    }

    fun tcTransactionSendError(id: Long? = null) {
        tcl.replyError(id ?: tcPendingSend!!.first, 6000, "Rejected by user")
    }

    fun tcTransactionSendResult(id: Long? = null) {
        val ps = tcPendingSend!!
        val wal = getWallet()!!
        val sent = mutableStateOf(false)
        crs.launch {
            multipass() { lc, _, _ ->
                val signed = wal.signTransfer(liteClient = lc,
                    transfers = ps.second.toTypedArray(), validUntil = ps.third)
                val boc = BagOfCells(signed).toByteArray().encodeBase64()
                synchronized(sent) {
                    if (!sent.value)
                        tcl.replySuccess(id ?: ps.first, boc)
                    sent.value = true
                }
                wal.transferPrepared(lc, signed)
                return@multipass true
            }
        }
    }

    fun tcSayHello(da: DAppItem, icrs: Map<String, String>) {
        val wal = getWallet()!!
        val devi = tcDeviceInfo()
        val items = mutableListOf<JsonObject>()
        if (icrs.containsKey("ton_addr")) {
            items.add(buildJsonObject {
                put("name", "ton_addr")
                put("address", wal.address.toString(false).lowercase())
                put("network", "-239")
                put("publicKey", wal.publicKey.key.encodeHex().lowercase())
                // DONE: Is it correct way to base64 encode cell? Could not find another way.
                // It IS correct! Tested with uninitialized empty wallet! Yoohoo!!
                put("walletStateInit", BagOfCells(wal.getStateInit().toCell()).toByteArray().encodeBase64())
            })
        }
        if (icrs.containsKey("ton_proof")) {
            val now = now()
            val payload = icrs["ton_proof"]!!

            val domain = da.domain()
            val dombts = domain.toByteArray()

            val inmsg =
                "ton-proof-item-v2/".toByteArray() +
                wal.workchainId.toBigEndian().toByteArray() +
                wal.address.address.toByteArray() +
                dombts.size.toLittleEndian().toByteArray() +
                dombts +
                now.toLittleEndian().toByteArray() +
                payload.toByteArray()

            val exmsg =
                hex("FFFF") + "ton-connect".toByteArray() + sha256(inmsg)

            val signature = wal.privateKey.sign(sha256(exmsg))

            items.add(buildJsonObject {
                put("name", "ton_proof")
                put("proof", buildJsonObject { // XXX: This sub-element ......
                    put("domain", buildJsonObject {
                        put("lengthBytes", domain.length)
                        put("value", domain)
                    })
                    put("signature", signature.encodeBase64())
                    put("payload", payload)
                    put("timestamp", now)
                })
            })
        }
        val obj = buildJsonObject {
            put("items", buildJsonArray {
                items.forEach { o -> add(o) }
            })
            put("device", devi)
        }
        tcl.sendEvent("connect", obj)
    }

    fun tcSayGoodbye(id: String) {
        val da = db.dappDao().get(id) ?: return
        TCLink.justSendEvent(http, hex(da.mykey), hex(da.id), FALLBACK_BRIDGE_URL,
            "disconnect", buildJsonObject {  })
    }

    fun parseInitialRequest(id: String, req: String, wal: String): Pair<DAppItem, Map<String, String>> {
        val rj = json.parseToJsonElement(req)
        val murl = rj.jsonObject["manifestUrl"]!!.jsonPrimitive.content
        val hreq = Request.Builder().url(murl).build()
        Log.i("DataMaster", "Parsing initial request, manifest $murl")
        val mani = http.newCall(hreq).execute().use { r ->
            if (!r.isSuccessful || r.body == null) {
                Log.w("DataMaster", "Failed to load manifest $murl")
                return@use null
            }
            return@use r.body!!.string()
        } ?: throw IllegalArgumentException("Failed to load manifest")
        val mj = json.parseToJsonElement(mani)
        val url = mj.jsonObject["url"]!!.jsonPrimitive.content
        val name = mj.jsonObject["name"]!!.jsonPrimitive.content
        val icon = mj.jsonObject["iconUrl"]!!.jsonPrimitive.content
        val tou = mj.jsonObject["termsOfUseUrl"]?.jsonPrimitive?.content
        val prp = mj.jsonObject["privacyPolicyUrl"]?.jsonPrimitive?.content
        val item = DAppItem(
            id = id,
            mykey = nacl.box.keyPair().second.encodeHex(),
            waladdr = wal,
            manifest = murl,
            url = url,
            name = name,
            iconUrl = icon,
            touUrl = tou,
            prpUrl = prp,
            lastAct = nowms(),
            active = false,
            closed = false,
            lastEvent = null
        )
        val items = rj.jsonObject["items"]?.jsonArray
        val icrs = items?.associate {
            Pair(
                it.jsonObject["name"]!!.jsonPrimitive.content,
                it.jsonObject["payload"]?.jsonPrimitive?.content ?: ""
            )
        } ?: mapOf()
        return Pair(item, icrs)
    }

}
