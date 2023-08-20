package com.github.oezeb.cypher_connect.design

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL


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
    companion object {
        const val CODES_URL = "https://flagcdn.com/en/codes.json"
        const val FLAG_WIDTH = 40

        fun getFlagUrl(code: String): String = "https://flagcdn.com/w${FLAG_WIDTH}/${code}.png"

        fun getFlagEmoji(countryCode: String): String {
            return countryCode
                .uppercase()
                .split("")
                .filter { it.isNotBlank() }
                .map { it.codePointAt(0) + 0x1F1A5 }
                .joinToString("") { String(Character.toChars(it)) }
        }
    }

    fun getCodes(): Map<String, *> {
        val filename = "codes.json"
        val path = File(context.getExternalFilesDir(null), "Download")
        if (!path.exists()) path.mkdirs()
        val file = File(path, filename)
        return if (file.exists()) {
            val data = file.readText()
            JSONObject(data).toMap()
        } else {
            file.createNewFile()
            val data = URL(CODES_URL).readText()
            file.writeText(data)
            JSONObject(data).toMap()
        }
    }

    fun getFlag(countryCode: String): Drawable? {
        val code = countryCode.lowercase()
        val url = getFlagUrl(code)
        val filename = "${code}.png"
        val path = File(context.cacheDir, "flags/w${FLAG_WIDTH}")
        if (!path.exists()) path.mkdirs()
        val file = File(path, filename)

        val options = BitmapFactory.Options()
        options.inDensity = DisplayMetrics.DENSITY_DEFAULT

        return try {
            val bitmap = if (file.exists()) {
                val data = file.readBytes()
                BitmapFactory.decodeByteArray(data, 0, data.size, options)
            } else {
                val data = URL(url).readBytes()
                file.writeBytes(data)
                BitmapFactory.decodeByteArray(data, 0, data.size, options)
            }
            BitmapDrawable(context.resources, bitmap)
        } catch (e: Exception) {
            null
        }
    }
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
