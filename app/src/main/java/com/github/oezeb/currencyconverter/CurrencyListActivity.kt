package com.github.oezeb.currencyconverter

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlin.concurrent.thread

class CurrencyListActivity : AppCompatActivity() {
    private val currencyManager = CurrencyManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_currency_list)

        val recyclerView = findViewById<RecyclerView>(R.id.recycle_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        thread {
            val favorites = currencyManager.getFavoriteSet().toMutableSet()
            val currencies = currencyManager.currencies.values.toList().sortedByDescending{
                favorites.contains(it.code)
            }

            CurrencyListAdapter.selectCurrencySet = favorites
            recyclerView.adapter = CurrencyListAdapter(currencies)
        }

        CurrencyListAdapter.onClickListener = {_, position, value ->
            val selected = CurrencyListAdapter.selectCurrencySet.orEmpty()
            if (selected.contains(value.code)) removeFavorite(value.code)
            else addFavorite(value.code)
            recyclerView.adapter?.notifyItemChanged(position)
        }


        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this)
        findViewById<AdView>(R.id.bannerAdView).apply { loadAd(AdRequest.Builder().build()) }
    }

    private fun addFavorite(code: String) {
        thread { currencyManager.addFavorite(code) }
        if (CurrencyListAdapter.selectCurrencySet == null) {
            CurrencyListAdapter.selectCurrencySet = mutableSetOf(code)
        } else {
            CurrencyListAdapter.selectCurrencySet!!.add(code)
        }
    }

    private fun removeFavorite(code: String) {
        thread { currencyManager.removeFavorite(code) }
        CurrencyListAdapter.selectCurrencySet?.remove(code)
    }

    fun popActivity(v: View) = finish()
}