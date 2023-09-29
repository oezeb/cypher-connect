package com.github.oezeb.cypher_connect.design

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.DisplayMetrics
import android.view.View
import android.widget.TextView
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL

@Parcelize
data class Server(val id: Long, val name: String?, var speed: Int?=null): Parcelable

@Parcelize
data class Location(
    val code: String?=null,
    val name: String,
    val servers: List<Server> = listOf(),
    var speed: Int?=null,
): Parcelable

fun TextView.setDrawable(drawable: Drawable?, index: Int) {
    val all = compoundDrawablesRelative
    all[index] = drawable
    setCompoundDrawablesRelativeWithIntrinsicBounds(all[0], all[1], all[2], all[3])
}

fun TextView.setDrawableStart(drawable: Drawable?) = setDrawable(drawable, 0)
fun TextView.setDrawableStart(drawable: Int) = setDrawableStart(context.getDrawable(drawable))
fun TextView.setDrawableEnd(drawable: Drawable?) = setDrawable(drawable, 2)
fun TextView.setDrawableEnd(drawable: Int) = setDrawableEnd(context.getDrawable(drawable))

/**
 * Get country flag from CDN
 * https://flagpedia.net
 */
class FlagCDN(private val context: Context) {
    fun getCodes(): Map<String, *> {
        val filename = "codes.json"
        val file = File(context.cacheDir, filename)
        return if (file.exists()) {
            val data = file.readText()
            JSONObject(data).toMap()
        } else {
            file.createNewFile()
            val url = context.getString(R.string.country_codes_url)
            val res = Http.get(url, timeout = 1000)
            if (res.ok) {
                val data = res.text
                file.writeText(data)
                JSONObject(data).toMap()
            } else {
                mapOf<String, Any>()
            }
        }
    }

    fun getFlag(countryCode: String): Drawable? {
        val code = countryCode.lowercase()
        val url = context.getString(R.string.flag_url, countryCode)
        val filename = "${code}.png"
        val path = File(context.cacheDir, "flags")
        if (!path.exists()) path.mkdirs()
        val file = File(path, filename)

        val options = BitmapFactory.Options()
        options.inDensity = DisplayMetrics.DENSITY_DEFAULT

        return try {
            val bitmap = if (file.exists()) {
                val data = file.readBytes()
                BitmapFactory.decodeByteArray(data, 0, data.size, options)
            } else {
                val res = Http.get(url, timeout = 1000)
                if (res.ok) {
                    val data = res.bytes
                    file.writeBytes(data)
                    BitmapFactory.decodeByteArray(data, 0, data.size, options)
                } else {
                    null
                }
            }
            BitmapDrawable(context.resources, bitmap)
        } catch (e: Exception) {
            null
        }
    }
}

val SPEED_ICONS = listOf(
    R.drawable.cellular_connection,
    R.drawable.cellular_connection_1,
    R.drawable.cellular_connection_2,
    R.drawable.cellular_connection_3,
    R.drawable.cellular_connection_4
)

fun updateSpeed(view: View, speed: Int?) {
    if (speed == null) return

    val index = when {
        speed < 0 -> 0
        speed >= SPEED_ICONS.size -> SPEED_ICONS.size - 1
        else -> speed
    }

    (view as TextView).setDrawableEnd(view.context.getDrawable(SPEED_ICONS[index]))
}

fun JSONObject.toMap(): Map<String, *> = keys().asSequence().associateWith {
    when (val value = get(it)) {
        is JSONObject -> value.toMap()
        is JSONArray -> value.toList()
        else -> value
    }
}

fun JSONArray.toList(): List<*> = (0 until length()).asSequence().map {
    when (val value = get(it)) {
        is JSONObject -> value.toMap()
        is JSONArray -> value.toList()
        else -> value
    }
}.toList()


class Http {
    companion object {
        data class Response(val code: Int, val bytes: ByteArray) {
            val ok get() = code in 200..299
            val text get() = String(bytes)
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Response

                if (code != other.code) return false
                if (!bytes.contentEquals(other.bytes)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = code
                result = 31 * result + bytes.contentHashCode()
                return result
            }
        }

        fun get(url: String, headers: Map<String, String>? = null, timeout: Int? = null): Response {
            val connection = URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            if (timeout != null) {
                connection.connectTimeout = timeout
                connection.readTimeout = timeout
            }
            headers?.forEach { connection.setRequestProperty(it.key, it.value) }
            val code = connection.responseCode
            val bytes = connection.inputStream.readBytes()
            return Response(code, bytes)
        }
    }
}
