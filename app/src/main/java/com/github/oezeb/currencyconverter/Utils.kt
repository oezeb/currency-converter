package com.github.oezeb.currencyconverter

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL

fun getFlag(context: Context, countryCode: String): Drawable? {
    val path = File(context.cacheDir, "flags")
    if (!path.exists()) path.mkdirs()

    val code = countryCode.lowercase()
    val url = context.getString(R.string.flag_url, code)
    val filename = "${code}.png"
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
        Log.d("CurrencyManager", "get flag exception $e", e)
        null
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