package app.quarkton.ton

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import app.quarkton.db.AppDatabase
import app.quarkton.db.DAppItem
import app.quarkton.ton.ext.nacl
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.sse.RealEventSource
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import org.ton.bitstring.BitString
import org.ton.crypto.SecureRandom
import org.ton.crypto.encodeHex
import org.ton.crypto.encoding.base64

class TCLink(
    val hc: OkHttpClient,
    val db: AppDatabase,
    val js: Json,
    val crs: CoroutineScope
): EventSourceListener() {

    var req: Request? = null
    var evs: EventSource? = null

    var cur: TCData? = null
    var new: TCData? = null

    var pub: ByteArray? = null
    var pri: ByteArray? = null
    var apk: ByteArray? = null

    var lastSentId: Long = 0L
    var lastRecvId: Long = -1L

    val isConnected = mutableStateOf(false)
    val isConnecting = mutableStateOf(false)
    val receivedAnything = mutableStateOf(false)
    var teardown = false

    suspend fun loop(): Boolean {
        val c = if (isConnected.value) cur else null
        val n = new
        isConnecting.value = false
        if (c != n) {
            // Actually any change will require disconnect and reconnect
            Log.i("TCLink", "Shifting state from ${c?.copy(mykey = "REDACTED")} to ${n?.copy(mykey = "REDACTED")}")
            try { evs?.cancel() } catch (_: Throwable) {}
            isConnected.value = false
            receivedAnything.value = false
            evs = null
            req = null
            cur = null
            delay(100)
            if (n != null) {
                val (pu, pk) = nacl.box.keyPairFromSecretKey(BitString(n.mykey).toByteArray())
                val ak = BitString(n.id).toByteArray()
                pri = pk
                pub = pu
                apk = ak
                if (n.connect) {
                    isConnecting.value = true
                    teardown = false
                    val da = db.dappDao().get(n.id)
                    val le = if (da?.lastEvent != null) "&last_event_id=${da.lastEvent}" else ""
                    val rq = Request.Builder()
                        .url(n.bridge + "events?client_id=" + pu.encodeHex() + le)
                        .build()
                    val ev = RealEventSource(rq, this)
                    req = rq
                    evs = ev
                    try {
                        lastRecvId = da?.lastEvent ?: -1L
                        ev.connect(hc)
                    } catch (e: Throwable) {
                        isConnecting.value = false
                        Log.e("TCLink", "SSE Connection failed", e)
                        return false
                    }
                }
                cur = n
            }
        }
        return true
    }

    override fun onOpen(eventSource: EventSource, response: Response) {
        Log.i("TCLink", "onOpen: " + response.message)
        isConnected.value = true
        isConnecting.value = false

    }

    override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
        Log.i("TCLink", "onEvent id:$id type:$type data:$data")
        receivedAnything.value = true
        val jd = js.parseToJsonElement(data)
        val from = (jd.jsonObject["from"] as? JsonPrimitive)?.content
        if (from == null) {
            Log.w("TCLink", "Warning: Missing from")
            return
        }
        if (from != cur?.id) {
            Log.w("TCLink", "Warning: Invalid from")
            return
        }
        val m = (jd.jsonObject["message"] as? JsonPrimitive)?.content
        if (m == null) {
            Log.w("TCLink", "Warning: Missing message")
            return
        }
        val rm = base64(m)
        val nonce = rm.take(24).toByteArray()
        val ct = rm.takeLast(rm.size - 24).toByteArray()
        val pt = nacl.box.open(ct, nonce, apk!!, pri!!)
        if (pt == null) {
            Log.e("TCLink", "NACL Box Open FAILED")
            return
        }
        val pts = pt.decodeToString()
        Log.i("TCLink", "Decrypted message: $pts")
        val md = js.parseToJsonElement(pts)
        val evid = md.jsonObject["id"]!!.jsonPrimitive.long
        if (evid <= lastRecvId) {
            Log.w("TCLink", "Warning: Ignoring old event ID $evid")
            return
        }
        lastRecvId = evid
        val method = md.jsonObject["method"]!!.jsonPrimitive.content
        val params = md.jsonObject["params"]
        handleEvent(method, params)
    }

    override fun onClosed(eventSource: EventSource) {
        Log.i("TCLink", "onClosed")
        isConnected.value = false

    }

    override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
        Log.i("TCLink", "onFailure ${t?.message} ${response?.message}", t)
        isConnected.value = false

    }

    fun handleEvent(method: String, params: JsonElement?) {
        if (method == "disconnect") {
            crs.launch {
                teardown = true
                db.dappDao().disconnect(cur!!.id)
            }
        }
    }

    fun sendEvent(name: String, data: JsonObject): Boolean {
        val c = cur
        val pu = pub
        val pk = pri
        val ak = apk
        if (c == null || ak == null || pk == null || pu == null) return false
        var id = nowms()
        if (id <= lastSentId)
            id = lastSentId + 1
        lastSentId = id
        val event = buildJsonObject {
            put("event", name)
            put("id", id)
            put("payload", data)
        }
        val pt = event.toString()
        val nonce = SecureRandom.nextBytes(24)
        val ct = (nonce + nacl.box.seal(pt.encodeToByteArray(), nonce, ak, pk)).encodeBase64()
        val req = Request.Builder()
            .url(c.bridge + "message?client_id=" + pu.encodeHex() + "&to=" + ak.encodeHex() + "&ttl=300")
            .post(ct.toRequestBody("application/json".toMediaType()))
            .build()
        Log.w("TCData", "Event req: $req")
        val res = hc.newCall(req).execute().use { r ->
            if (!r.isSuccessful || r.body == null) {
                Log.w("TCData", "HTTP Request failed")
                return@use null
            }
            return@use r.body!!.string()
        }
        Log.i("TCData", "Event response: $res")
        return true
    }

}

data class TCData(
    val id: String,
    val mykey: String,
    val bridge: String,
    val connect: Boolean
) {

    companion object {

        fun fromItem(item: DAppItem, bridgeKey: String, connect: Boolean = true): TCData =
            TCData(item.id, item.mykey, DataMaster.bridges[bridgeKey]?.first ?:
                throw IllegalArgumentException("Bridge not in config"), connect)

    }

}