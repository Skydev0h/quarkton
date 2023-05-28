package app.quarkton.ton

import android.util.Log
import app.quarkton.ton.extensions.storeEmbedded
import app.quarkton.ton.extensions.toRepr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import org.ton.api.pk.PrivateKeyEd25519
import org.ton.api.pub.PublicKeyEd25519
import org.ton.block.AccountActive
import org.ton.block.AccountInfo
import org.ton.block.AddrNone
import org.ton.block.AddrStd
import org.ton.block.Coins
import org.ton.block.CommonMsgInfoRelaxed
import org.ton.block.Either
import org.ton.block.ExtInMsgInfo
import org.ton.block.Maybe
import org.ton.block.Message
import org.ton.block.MessageRelaxed
import org.ton.block.StateInit
import org.ton.cell.Cell
import org.ton.cell.CellBuilder
import org.ton.contract.wallet.WalletTransfer
import org.ton.lite.client.LiteClient
import org.ton.lite.client.internal.FullAccountState
import org.ton.lite.client.internal.TransactionId
import org.ton.lite.client.internal.TransactionInfo
import org.ton.tlb.CellRef
import org.ton.tlb.constructor.AnyTlbConstructor
import org.ton.tlb.storeRef
import kotlin.math.abs

class Wallet(
    val verRev: Int,
    val privateKey: PrivateKeyEd25519,
    val workchainId: Int = 0,
    val walletId: Int = IDBase + workchainId,
    val messageTimeout: Int = 600
) {

    data class WalletData(
        val lastUpdated: Long? = null,
        val lastServTime: Instant? = null,
        val fullState: FullAccountState? = null,
        val info: AccountInfo? = null,
        val state: StateInit? = null,
        val data: Cell? = null,
        val balance: Coins? = null,
        val seqno: UInt = 0u
    )

    val ver = verRev.shr(8)
    val publicKey: PublicKeyEd25519 = privateKey.publicKey()
    val address: AddrStd = getAddress(verRev, publicKey, workchainId, walletId)

    // val code: Cell = getCode(verRev)
    val initData: Cell = getInitData(verRev, publicKey, walletId)

    private val _data = MutableStateFlow(WalletData())
    val data = _data as StateFlow<WalletData>

    suspend fun update(liteClient: LiteClient) {
        val fullState = liteClient.getAccountState(address)
        val servTime = liteClient.getServerTime()
        val now = nowms()
        val info = fullState.account.value as? AccountInfo
        val state = (info?.storage?.state as? AccountActive)?.value
        val data = state?.data?.value?.value
        val balance = info?.storage?.balance?.coins
        val seqno: UInt = data?.beginParse()?.loadUInt32() ?: 0u
        _data.value = WalletData(now, servTime, fullState, info, state, data, balance, seqno)
    }

    suspend fun getTransactions(liteClient: LiteClient, count: Int = 50, after: TransactionId? = null): List<TransactionInfo> {
        if (_data.value.lastUpdated == null) update(liteClient) // need lastTransactionId
        val data = _data.value
        val lti = after ?: (data.fullState?.lastTransactionId ?: return listOf())
        Log.i("Wallet", "getTransactions lti: $lti")
        try {
            val trans = liteClient.getTransactions(address, lti, count)
            // DONE: Verify transaction chain
            // Root of trust: Account LastTransactionId verified by proof
            // It contains link to previous transactions that makes a chain of trust
            trans.forEachIndexed { i, it ->
                if (it.transaction.hash() != it.id.hash) {
                    // Make sure hash in id matches actual cell hash
                    throw SecurityException("Transaction hash does not match transaction id")
                }
                // Can have ID = LTI or one of other elements, must form chain!
                if (it.id == lti) return@forEachIndexed
                if (i > 0) { // Let's first try previous entry, most like it will be it!

                    with (trans[i-1].transaction.value) {
                        if ((prevTransHash == it.id.hash) && (prevTransLt.toLong() == it.id.lt))
                            return@forEachIndexed
                    }
                }
                if (i < trans.size - 1) { // Reversed order? Check previous item
                    with (trans[i+1].transaction.value) {
                        if ((prevTransHash == it.id.hash) && (prevTransLt.toLong() == it.id.lt))
                            return@forEachIndexed
                    }
                }
                // Try to find parent in chain anywhere
                // NB: Since maximum 50 transactions per request, maximum square complexity 2500
                // In fact usually lite server returns much less data (<20) - about 400 SQ max
                if (trans.any { o -> with (o.transaction.value) {
                        (prevTransHash == it.id.hash) && (prevTransLt.toLong() == it.id.lt) } })
                    return@forEachIndexed
                throw SecurityException("Chain broken - ${it.id.toRepr()} has no viable parent")
                // Should not think about possible faking circular chains since that will require
                // colliding sha256 hash because cell contents hash is checked against ID
            }
            // Validate transactions chain to make sure that nothing is forged or corrupted
            // This relates to checking proofs, because account state proofs are checked
            // And for transactions, their hashes are verified to match contents in lib
            // So, if transaction chain is verified, we can be sure nothing is forged
            return trans
        } catch (t: Throwable) {
            Log.e("Wallet", "getTransactions crashed with ", t)
            return listOf()
        }
    }

    fun getStateInit() =
        getStateInit(verRev, publicKey, workchainId, walletId)

    suspend fun signTransfer(liteClient: LiteClient, vararg transfers: WalletTransfer, validUntil: Long? = null): Cell {
        if (transfers.size > 4)
            throw IllegalArgumentException("Maximum of 4 transfers at a time are supported")

        update(liteClient) // Make sure seqNo is actual

        val data = _data.value

        val expiry = validUntil ?: if (data.seqno == 0u) 0xFFFFFFFFL else
            (data.lastServTime?.epochSeconds ?: now()) + messageTimeout

        val body = CellBuilder.createCell {
            if (ver >= 3) {
                storeUInt(walletId, 32)
                storeUInt(expiry, 32)
            }
            storeUInt32(data.seqno)
            if (ver == 2) storeUInt(expiry, 32)
            if (ver >= 4) storeUInt(0, 8) // op
            transfers.forEach { transfer ->
                val sendMode = if (transfer.sendMode > -1) transfer.sendMode else 3
                storeUInt(sendMode, 8)
                storeRef(MessageRelaxed.tlbCodec(AnyTlbConstructor), CellRef(intMsg(transfer)))
            }
        }

        val signed = CellBuilder.createCell {
            storeBytes(privateKey.sign(body.hash().toByteArray()))
            storeEmbedded(body)
        }

        return signed
    }

    suspend fun transferPrepared(liteClient: LiteClient, prepared: Cell) {
        val data = _data.value

        val extMsgInfo = ExtInMsgInfo(src = AddrNone, dest = address, importFee = Coins())

        val stateInit: StateInit? = if (data.state != null) null else getStateInit().value
        val maybeStateInit = Maybe.of(stateInit?.let { Either.of<StateInit, CellRef<StateInit>>(null, CellRef(it)) })

        val bodyRef = Either.of<Cell, CellRef<Cell>>(null, CellRef(prepared))

        liteClient.sendMessage(Message(info = extMsgInfo, init = maybeStateInit, body = bodyRef))
    }

    suspend fun transfer(liteClient: LiteClient, vararg transfers: WalletTransfer, validUntil: Long? = null) {
        if (transfers.size > 4)
            throw IllegalArgumentException("Maximum of 4 transfers at a time are supported")

        update(liteClient) // Make sure seqNo is actual

        transferPrepared(liteClient, signTransfer(liteClient = liteClient, transfers = transfers, validUntil = validUntil))
    }

    suspend fun transferClassic(liteClient: LiteClient, vararg transfers: WalletTransfer) {
        if (transfers.size > 4)
            throw IllegalArgumentException("Maximum of 4 transfers at a time are supported")

        update(liteClient) // Make sure seqNo is actual
        val data = _data.value

        val extMsgInfo = ExtInMsgInfo(src = AddrNone, dest = address, importFee = Coins())

        val stateInit: StateInit? = if (data.state != null) null else getStateInit().value
        val maybeStateInit = Maybe.of(stateInit?.let { Either.of<StateInit, CellRef<StateInit>>(null, CellRef(it)) })

        val expiry = if (data.seqno == 0u) 0xFFFFFFFFL else
            (data.lastServTime?.epochSeconds ?: now()) + messageTimeout

        val body = CellBuilder.createCell {
            if (ver >= 3) {
                storeUInt(walletId, 32)
                storeUInt(expiry, 32)
            }
            storeUInt32(data.seqno)
            if (ver == 2) storeUInt(expiry, 32)
            if (ver >= 4) storeUInt(0, 8) // op
            transfers.forEach { transfer ->
                val sendMode = if (transfer.sendMode > -1) transfer.sendMode else 3
                storeUInt(sendMode, 8)
                storeRef(MessageRelaxed.tlbCodec(AnyTlbConstructor), CellRef(intMsg(transfer)))
            }
        }

        val signed = CellBuilder.createCell {
            storeBytes(privateKey.sign(body.hash().toByteArray()))
            storeEmbedded(body)
        }

        val bodyRef = Either.of<Cell, CellRef<Cell>>(null, CellRef(signed))

        liteClient.sendMessage(Message(info = extMsgInfo, init = maybeStateInit, body = bodyRef))
    }

    fun deprecated(): Boolean = Companion.deprecated(ver)

    companion object {

        const val IDBase = 698983191

        const val V1R1 = 0x101
        const val V1R2 = 0x102
        const val V1R3 = 0x103
        const val V1Latest = 0x1FF

        const val V2R1 = 0x201
        const val V2R2 = 0x202
        const val V2Latest = 0x2FF

        const val V3R1 = 0x301
        const val V3R2 = 0x302
        const val V3Latest = 0x3FF

        const val V4R1 = 0x401
        const val V4R2 = 0x402
        const val V4Latest = 0x4FF

        val useWallets = arrayOf(
            V4R2, V4R1,
            V3R2, V3R1,
            V2R2, V2R1,
            V1R3, V1R2, V1R1
        )

        // const val HighloadV2 = 0x82FF

        const val Latest = 0xFFFF

        fun deprecated(ver: Int): Boolean = (ver <= 2)
        fun vrdeprecated(verRev: Int): Boolean =
            deprecated(abs(verRev).shr(8)) or (abs(verRev) == V4R1)

        fun getStateInit(code: Cell, data: Cell): CellRef<StateInit> =
            CellRef(StateInit(code, data), StateInit)

        fun getAddress(
            verRev: Int,
            publicKey: PublicKeyEd25519,
            workchainId: Int = 0,
            walletId: Int? = null
        ): AddrStd =
            AddrStd(workchainId, getStateInit(verRev, publicKey, workchainId, walletId).hash())

        fun getStateInit(
            verRev: Int,
            publicKey: PublicKeyEd25519,
            workchainId: Int = 0,
            walletId: Int? = null
        ): CellRef<StateInit> = getStateInit(
            CodeMaster.getWallet(verRev),
            getInitData(verRev, publicKey, walletId ?: (IDBase + workchainId))
        )

        fun getInitData(
            verRev: Int,
            publicKey: PublicKeyEd25519,
            walletId: Int = IDBase
        ): Cell = CellBuilder.createCell {
            val ver = verRev.ushr(8)
            storeUInt(0, 32) // seqno: UInt32 = 0
            if (ver >= 3)
                storeUInt(walletId, 32)
            storeBytes(publicKey.key.toByteArray())
            if (ver >= 4)
                storeUInt(0, 1) // plugin dict (empty)
        }

        fun intMsg(transfer: WalletTransfer): MessageRelaxed<Cell> {
            val info = CommonMsgInfoRelaxed.IntMsgInfoRelaxed(
                bounce = transfer.bounceable, src = AddrNone,
                dest = transfer.destination,  value = transfer.coins,

                ihrDisabled = true, bounced = false, ihrFee = Coins(),
                fwdFee = Coins(),   createdLt = 0u,  createdAt = 0u
            )

            val init = Maybe.of(transfer.messageData.stateInit?.let {
                Either.of<StateInit, CellRef<StateInit>>(null, it)
            })
            val bodyCell = transfer.messageData.body
            // Will return left in priority if it is not null, else will return right
            val body = Either.of(if (bodyCell.isEmpty()) bodyCell else null, CellRef(bodyCell))
            return MessageRelaxed(info = info, init = init, body = body)
        }
    }

}

fun Int.walVer(rev: Int = 0xFF): Int {
    return (this * 0x100 + rev)
}