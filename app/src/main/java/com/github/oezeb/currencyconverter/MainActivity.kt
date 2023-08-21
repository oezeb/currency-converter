package com.github.oezeb.currencyconverter

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {
    private val currencyManager = CurrencyManager(this)

    private lateinit var baseSpinner: Spinner
    private lateinit var baseTextField: TextInputEditText
    private lateinit var quoteSpinner: Spinner
    private lateinit var quoteTextField: TextInputEditText
    private lateinit var rateView: TextView

    private val defaultBaseValue = "1.00"

    private val editListActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            thread {
                val currencies = currencyManager.getFavoriteCurrencies().values.toList()
                Handler(Looper.getMainLooper()).post { setSpinnerCurrencies(currencies) }
            }
        }

    private val onBaseItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(p0: AdapterView<*>?) = Unit
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val item = baseSpinner.getItemAtPosition(position) as Currency
            thread { currencyManager.setBaseCurrency(item.code) }
            updateRate()
        }
    }

    private val onQuoteItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(p0: AdapterView<*>?) = Unit
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val item = quoteSpinner.getItemAtPosition(position) as Currency
            thread { currencyManager.setQuoteCurrency(item.code) }
            updateRate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SpinnerAdapter.getLag = { countryCode -> currencyManager.getFlag(countryCode) }

        findViewById<LinearLayout>(R.id.base_currency_row).let { baseRow ->
            baseTextField = baseRow.findViewById(R.id.convert_row_textfield)
            baseSpinner = baseRow.findViewById(R.id.convert_row_spinner)

            baseTextField.apply {
                setText(defaultBaseValue)
                addTextChangedListener { updateRate() }
            }
            baseSpinner.onItemSelectedListener = onBaseItemSelectedListener
        }

        findViewById<LinearLayout>(R.id.quote_currency_row).let { quoteRow ->
            quoteTextField = quoteRow.findViewById(R.id.convert_row_textfield)
            quoteSpinner = quoteRow.findViewById(R.id.convert_row_spinner)
            quoteRow.findViewById<TextView>(R.id.convert_row_text).text =
                getString(R.string.converted_amount)

            quoteTextField.isFocusable = false
            quoteSpinner.onItemSelectedListener = onQuoteItemSelectedListener
        }

        rateView = findViewById(R.id.rate)

        thread {
            val currencies = currencyManager.getFavoriteCurrencies().values.toList()
            Handler(Looper.getMainLooper()).post {
                setSpinnerCurrencies(currencies)
                updateRate()
            }
        }

        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this)
        findViewById<AdView>(R.id.bannerAdView).apply { loadAd(AdRequest.Builder().build()) }
    }


    private fun updateRate() {
        val value = baseTextField.text.toString()
        if (value.isEmpty()) return

        val base = (baseSpinner.selectedItem as Currency).code
        val quote = (quoteSpinner.selectedItem as Currency).code

        thread {
            val (rate, date) = currencyManager.getRate(base, quote)
            Handler(Looper.getMainLooper()).post {
                if (rate == null) {
                    quoteTextField.setText("")
                    rateView.text = getString(R.string.no_rate_available)
                } else {
                    val converted = value.toDouble() * rate
                    quoteTextField.setText(String.format("%.2f", converted))

                    var rateViewText = String.format(
                        "1 %s = %.2f %s",
                        base.uppercase(),
                        rate,
                        quote.uppercase()
                    )

                    val today = dateFormat.parse(dateFormat.format(Date()))
                    rateViewText += if (date != null && date != today) {
                        val formattedDate = SimpleDateFormat.getDateInstance().format(date)
                        " ($formattedDate)"
                    } else ""

                    rateView.text = rateViewText
                }
            }
        }
    }

    private fun setSpinnerSelection(spinner: Spinner, position: Int, type: CurrencyType) {
        val item = spinner.getItemAtPosition(position) as Currency
        thread {
            when (type) {
                CurrencyType.Base -> currencyManager.setBaseCurrency(item.code)
                CurrencyType.Quote -> currencyManager.setQuoteCurrency(item.code)
            }
        }
        spinner.setSelection(position)
    }

    private fun setSpinnerCurrencies(currencies: List<Currency>) {
        baseSpinner.adapter = SpinnerAdapter(currencies)
        quoteSpinner.adapter = SpinnerAdapter(currencies)

        val base = currencyManager.getBaseCurrency()
        val quote = currencyManager.getQuoteCurrency()
        Log.d("Main", "Base: $base, Quote: $quote")
        if (base != null && quote != null) {
            val basePosition = currencies.indexOfFirst { it.code == base }
            val quotePosition = currencies.indexOfFirst { it.code == quote }
            baseSpinner.setSelection(basePosition)
            quoteSpinner.setSelection(quotePosition)
        }
    }

    fun showPopup(view: View) {
        PopupMenu(this, view).apply {
            setOnMenuItemClickListener(this@MainActivity)
            inflate(R.menu.app_menu)
            show()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit_currency_list -> {
                val intent = Intent(this, CurrencyListActivity::class.java)
                editListActivityLauncher.launch(intent)
                true
            }
            R.id.about -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun swapCurrency(v: View) {
        val basePosition = baseSpinner.selectedItemPosition
        val quotePosition = quoteSpinner.selectedItemPosition
        setSpinnerSelection(baseSpinner, quotePosition, CurrencyType.Base)
        setSpinnerSelection(quoteSpinner, basePosition, CurrencyType.Quote)
    }
}