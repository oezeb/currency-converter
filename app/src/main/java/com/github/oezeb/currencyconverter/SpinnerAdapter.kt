package com.github.oezeb.currencyconverter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import kotlin.concurrent.thread

class SpinnerAdapter(private val data: List<Currency>): BaseAdapter() {
    override fun getCount(): Int = data.size
    override fun getItem(position: Int): Any = data[position]
    override fun getItemId(position: Int): Long = position.toLong()

    private fun getItemView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return if (convertView != null) convertView
        else {
            val inflater = LayoutInflater.from(parent?.context)

            inflater.inflate(R.layout.currency_list_item, parent, false).apply {
                findViewById<TextView>(R.id.currency).text =
                    data[position].code.uppercase()
                findViewById<TextView>(R.id.currency_detail).text =
                    data[position].name
                val flagView = findViewById<ImageView>(R.id.flag)
                thread {
                    val flag = parent?.context?.let { getFlag(it, data[position].countryCode) }
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        flagView.setImageDrawable(flag)
                    }
                }
            }
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View =
        getItemView(position, convertView, parent).apply {
            findViewById<TextView>(R.id.currency_detail).visibility = View.GONE
            findViewById<ImageView>(R.id.right_icon)
                .setImageResource(R.drawable.baseline_keyboard_arrow_down_24)
        }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View =
        getItemView(position, convertView, parent).apply {
            findViewById<TextView>(R.id.currency_detail).visibility = View.VISIBLE
            findViewById<ImageView>(R.id.right_icon).visibility = View.GONE
        }
}