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
    val crs: CoroutineScope,
    val evup: (method: String, params: JsonElement?, id: Long) -> Pair<Int?, String>
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
        updateLastAct()
        isConnected.value = true
        isConnecting.value = false
    }

    override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
        Log.i("TCLink", "onEvent id:$id type:$type data:$data")
        updateLastAct()
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
        lastRecvId = evid // Persistent one should be updated after handling the event
        val method = md.jsonObject["method"]!!.jsonPrimitive.content
        val params = md.jsonObject["params"]
        handleEvent(method, params, evid)
    }

    override fun onClosed(eventSource: EventSource) {
        Log.i("TCLink", "onClosed")
        updateLastAct()
        isConnected.value = false
    }

    override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
        Log.i("TCLink", "onFailure ${t?.message} ${response?.message}", t)
        updateLastAct()
        isConnected.value = false
    }

    fun updateLastAct() {
        crs.launch { db.dappDao().updateLastAct(cur?.id ?: return@launch, nowms()) }
    }

    fun eventHandled(id: Long) {
        if (id > lastRecvId) return // WTF?
        crs.launch {
            try {
                db.dappDao().updateLastEvent(cur!!.id, id)
                Log.i("TCLink", "LastEvent of ${cur!!.id} updated to $id")
            } catch (_: Throwable) {}
        }
    }

    fun handleEvent(method: String, params: JsonElement?, id: Long) {
        Log.i("TCLink", "Handling incoming event/request: $id | $method")
        when (method) {
            "sendTransaction" -> {
                eventHandled(id)
                try {
                    val res = evup(method, params, id)
                    if (res.first == null)
                        replySuccess(id, res.second)
                    else if (res.first!! >= 0)
                        replyError(id, res.first!!, res.second)
                    /* else {
                        if (res.first == -69)
                            evs?.cancel()
                    } */
                } catch (e: Throwable) {
                    Log.e("TCLink", "Upper handler crashed", e)
                    replyError(id, 5000, "General protection fault")
                }
            }
            "disconnect" -> {
                eventHandled(id)
                crs.launch {
                    teardown = true
                    db.dappDao().disconnect(cur!!.id)
                }
            }
            else -> {
                Log.w("TCLink", "Warning: unknown method $method with params $params")
                eventHandled(id)
                replyError(id, 4040, "Unknown method (Lower layer)")
            }
        }
    }

    fun replySuccess(id: Long, data: String): Boolean {
        Log.w("TCLink", "Sending success response to $id: $data")
        return sendObject(buildJsonObject {
            put("id", id.toString())
            put("result", data)
        })
    }

    fun replyError(id: Long, code: Int, message: String): Boolean {
        Log.w("TCLink", "Sending error response to $id: ($code) $message")
        return sendObject(buildJsonObject {
            put("id", id.toString())
            put("error", buildJsonObject {
                put("code", code)
                put("message", message)
            })
        })
    }

    fun sendObject(data: JsonObject): Boolean {
        return justSendObject(
            hc, pri ?: return false, apk ?: return false,
            cur?.bridge ?: return false, data
        )
    }

    fun sendEvent(name: String, data: JsonObject): Boolean {
        return justSendEvent(
            hc, pri ?: return false, apk ?: return false,
            cur?.bridge ?: return false, name, data
        )
        /*
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
        */
    }

    companion object {

        fun justSendObject(hc: OkHttpClient, privKey: ByteArray, appKey: ByteArray,
                           bridge: String, data: JsonObject): Boolean {
            val pu = nacl.box.keyPairFromSecretKey(privKey).first
            val pt = data.toString()
            val nonce = SecureRandom.nextBytes(24)
            val ct = (nonce + nacl.box.seal(pt.encodeToByteArray(), nonce, appKey, privKey)).encodeBase64()
            val req = Request.Builder()
                .url(bridge + "message?client_id=" + pu.encodeHex() + "&to=" + appKey.encodeHex() + "&ttl=300")
                .post(ct.toRequestBody("application/json".toMediaType()))
                .build()
            Log.i("TCData", "Sending: $req")
            val res = hc.newCall(req).execute().use { r ->
                if (!r.isSuccessful || r.body == null) {
                    Log.w("TCData", "HTTP Request failed")
                    return@use null
                }
                return@use r.body!!.string()
            }
            Log.i("TCData", "Response: $res")
            return true
        }

        fun justSendEvent(hc: OkHttpClient, privKey: ByteArray, appKey: ByteArray,
                          bridge: String, name: String, data: JsonObject): Boolean {
            val event = buildJsonObject {
                put("event", name)
                put("id", nowms())
                put("payload", data)
            }
            return justSendObject(hc, privKey, appKey, bridge, event)
        }

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