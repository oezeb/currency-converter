package com.github.oezeb.currencyconverter

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread


val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

class Currency(val code: String, val name: String, val countryCode: String)

class CurrencyManager(private val context: Context) {
    companion object {
        const val PREFERENCES = "CurrencyPreferences"

        private const val FAVORITE_CURRENCIES_KEY = "FavoriteCurrencies"
        private const val BASE_CURRENCY_KEY = "BaseCurrency"
        private const val QUOTE_CURRENCY_KEY = "QuoteCurrency"

        val currencyPairKey: (String, String) -> String = { base, quote -> "$base/$quote" }
        val currencyPairDateKey: (String, String) -> String = { base, quote -> "$base/$quote-date" }

        val defaultFavoriteSet = setOf("usd", "eur", "jpy", "gbp", "cny")
        fun getFavoriteCurrencies(sharedPreferences: SharedPreferences): Set<String>? =
            sharedPreferences.getStringSet(FAVORITE_CURRENCIES_KEY, null)

        fun addFavorite(sharedPreferences: SharedPreferences, code: String) {
            sharedPreferences.edit().putStringSet(
                FAVORITE_CURRENCIES_KEY,
                getFavoriteCurrencies(sharedPreferences).orEmpty() + code
            ).apply()
        }

        fun removeFavorite(sharedPreferences: SharedPreferences, code: String) {
            sharedPreferences.edit().putStringSet(
                FAVORITE_CURRENCIES_KEY,
                getFavoriteCurrencies(sharedPreferences).orEmpty() - code
            ).apply()
        }

        fun getBaseCurrency(sharedPreferences: SharedPreferences): String? =
            sharedPreferences.getString(BASE_CURRENCY_KEY, null)
        
        fun setBaseCurrency(sharedPreferences: SharedPreferences, code: String) {
            sharedPreferences.edit().putString(BASE_CURRENCY_KEY, code).apply()
        }

        fun getQuoteCurrency(sharedPreferences: SharedPreferences): String? =
            sharedPreferences.getString(QUOTE_CURRENCY_KEY, null)

        fun setQuoteCurrency(sharedPreferences: SharedPreferences, code: String) {
            sharedPreferences.edit().putString(QUOTE_CURRENCY_KEY, code).apply()
        }

        fun getRate(sharedPreferences: SharedPreferences, base: String, quote: String): Pair<Double?, Date?> {
            val today = dateFormat.parse(dateFormat.format(Date()))
            if (base == quote) return  1.0 to today

            val key = currencyPairKey(base, quote)
            val dateKey = currencyPairDateKey(base, quote)
            val rate = sharedPreferences.getFloat(key, -1f)
            val date = sharedPreferences.getLong(dateKey, -1L)

            return if (rate != -1f && date == today.time) rate.toDouble() to today
            else null to null
        }

        fun setRate(sharedPreferences: SharedPreferences, base: String, quote: String, rate: Double, date: Date) {
            val key = currencyPairKey(base, quote)
            val dateKey = currencyPairDateKey(base, quote)
            sharedPreferences.edit().putFloat(key, rate.toFloat()).apply()
            sharedPreferences.edit().putLong(dateKey, date.time).apply()
        }
    }

    private val sharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    }

    val currencies: Map<String, Currency> by lazy {
        context.resources.openRawResource(R.raw.currencies).use { stream ->
            JSONObject(String(stream.readBytes())).toMap().mapValues {
                val value = it.value as Map<*, *>
                Currency(it.key, value["name"] as String, value["country"] as String)
            }
        }
    }

    private lateinit var _favoriteCurrencies: Set<String>
    val favoriteCurrencies: Set<String>
        get() {
            if (!::_favoriteCurrencies.isInitialized) {
                _favoriteCurrencies = getFavoriteCurrencies(sharedPreferences).orEmpty()
                if (_favoriteCurrencies.isEmpty()) {
                    thread {
                        for (code in defaultFavoriteSet) addFavorite(sharedPreferences, code)
                    }
                    _favoriteCurrencies = defaultFavoriteSet
                }
            }
            return _favoriteCurrencies
        }

    private lateinit var _baseCurrency: String
    var baseCurrency: String
        get() {
            if (!::_baseCurrency.isInitialized) {
                _baseCurrency = getBaseCurrency(sharedPreferences).orEmpty()
                if (_baseCurrency.isEmpty()) {
                    _baseCurrency = favoriteCurrencies.firstOrNull().orEmpty()
                    thread { setBaseCurrency(sharedPreferences, _baseCurrency) }
                }
            }
            return _baseCurrency
        }
        set(value) {
            _baseCurrency = value
            thread { setBaseCurrency(sharedPreferences, value) }
        }

    private lateinit var _quoteCurrency: String
    var quoteCurrency: String
        get() {
            if (!::_quoteCurrency.isInitialized) {
                _quoteCurrency = getQuoteCurrency(sharedPreferences).orEmpty()
                if (_quoteCurrency.isEmpty()) {
                    _quoteCurrency = favoriteCurrencies.lastOrNull().orEmpty()
                    thread { setQuoteCurrency(sharedPreferences, _quoteCurrency) }
                }
            }
            return _quoteCurrency
        }
        set(value) {
            _quoteCurrency = value
            thread { setQuoteCurrency(sharedPreferences, value) }
        }

    fun addFavorite(code: String) {
        thread { addFavorite(sharedPreferences, code) }
        _favoriteCurrencies += code
    }

    fun removeFavorite(code: String) {
        thread {  removeFavorite(sharedPreferences, code) }
        _favoriteCurrencies -= code
    }

    fun getRate(base: String, quote: String): Pair<Double?, Date?> {
        val (rate, date) = getRate(sharedPreferences, base, quote)
        return if (rate != null && date != null) rate to date
        else {
            val url = context.getString(R.string.currency_rate_url, base, quote)
            try {
                val data = URL(url).readText()
                val json = JSONObject(data)
                val newRate = json.getDouble(quote)
                val newDate = dateFormat.parse(json.getString("date"))
                thread { setRate(sharedPreferences, base, quote, newRate, newDate) }
                newRate to newDate
            } catch (e: Exception) {
                Log.d("CurrencyManager", "getRate Exception", e)
                null to null
            }
        }
    }
}
