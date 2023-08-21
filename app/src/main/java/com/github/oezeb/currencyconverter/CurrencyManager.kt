package com.github.oezeb.currencyconverter

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

enum class CurrencyType { Base, Quote }

class Currency(val code: String, val name: String, val countryCode: String)

class CurrencyManager(private val context: Context) {
    companion object {
        private const val FAVORITES = "Favorites"
        private val defaultFavoriteSet = setOf("usd", "eur", "jpy", "gbp", "cny")

        /// Get Currency rate from https://github.com/fawazahmed0/currency-api
        private const val API_URL = "https://cdn.jsdelivr.net/gh/fawazahmed0/currency-api@1"
        private fun getRateUrl(base: String, quote: String): String {
            return "$API_URL/latest/currencies/$base/$quote.json"
        }

        /// Get country flag from https://flagpedia.net
        private const val FLAG_WIDTH = 80
        private fun getFlagUrl(code: String): String {
            return "https://flagcdn.com/w${FLAG_WIDTH}/${code}.png"
        }
    }

    private val sharedPreferences by lazy {
        context.getSharedPreferences("Prefs", Context.MODE_PRIVATE)
    }

    val currencies: Map<String, Currency> by lazy {
        context.resources.openRawResource(R.raw.currencies).use { stream ->
            JSONObject(String(stream.readBytes())).toMap().mapValues {
                val value = it.value as Map<*, *>
                Currency(it.key, value["name"] as String, value["country"] as String)
            }
        }
    }

    fun getFavoriteSet(): Set<String> {
        Log.d("Currency Manager", "count: ${currencies.size}")
        val favorites = sharedPreferences.getStringSet(FAVORITES, null)?.toSet()
        return if (favorites == null) {
            for (code in defaultFavoriteSet) addFavorite(code)
            defaultFavoriteSet
        } else {
            favorites
        }
    }

    fun getFavoriteCurrencies(): Map<String, Currency> =
        currencies.filter { getFavoriteSet().contains(it.key) }

    fun addFavorite(code: String) {
        sharedPreferences.edit().putStringSet(FAVORITES, getFavoriteSet() + code).apply()
    }

    fun removeFavorite(code: String) {
        sharedPreferences.edit().putStringSet(FAVORITES, getFavoriteSet() - code).apply()
    }

    fun getBaseCurrency(): String? {
        return sharedPreferences.getString("base", null) ?: getFavoriteSet().firstOrNull()
    }

    fun setBaseCurrency(code: String) {
        sharedPreferences.edit().putString("base", code).apply()
    }

    fun getQuoteCurrency(): String? {
        return sharedPreferences.getString("quote", null) ?: getFavoriteSet().lastOrNull()
    }

    fun setQuoteCurrency(code: String) {
        sharedPreferences.edit().putString("quote", code).apply()
    }

    fun getRate(base: String, quote: String): Pair<Double?, Date?> {
        val today = dateFormat.parse(dateFormat.format(Date()))
        if (base == quote) return  1.0 to today

        val key = "$base/$quote"
        val dateKey = "$key-date"
        val rate = sharedPreferences.getFloat(key, -1f)
        val date = sharedPreferences.getLong(dateKey, -1L)
        if (rate != -1f && date == today.time) return rate.toDouble() to today

        val url = getRateUrl(base, quote)
        try {
            val data = URL(url).readText()
            val json = JSONObject(data)
            val newRate = json.getDouble(quote)
            val newDate = dateFormat.parse(json.getString("date"))
            sharedPreferences.edit().putFloat(key, newRate.toFloat()).apply()
            if (newDate != null) {
                sharedPreferences.edit().putLong(dateKey, newDate.time).apply()
                return newRate to newDate
            }
        } catch (e: Exception) {
            Log.d("CurrencyManager", "getRate Exception", e)
        }

        return if (rate != -1f && date != -1L) rate.toDouble() to Date(date)
        else null to null
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
            Log.d("CurrencyManager", "get flag exception $e", e)
            null
        }
    }
}