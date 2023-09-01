package com.github.oezeb.currencyconverter

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val html = resources.openRawResource(R.raw.about).bufferedReader().use { it.readText() }
        findViewById<TextView>(R.id.about_text).apply {
            movementMethod = LinkMovementMethod.getInstance()
            text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true);

        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this)
        findViewById<AdView>(R.id.bannerAdView).apply { loadAd(AdRequest.Builder().build()) }
    }
}