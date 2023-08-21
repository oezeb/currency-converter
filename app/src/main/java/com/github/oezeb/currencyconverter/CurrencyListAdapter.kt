package com.github.oezeb.currencyconverter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread

class CurrencyListAdapter(private val data: List<Currency>)
    : RecyclerView.Adapter<CurrencyListAdapter.ViewHolder>() {
    companion object {
        var selectCurrencySet: MutableSet<String>? = null
        var onClickListener: ((ViewHolder, Int, Currency) -> Unit)? = null
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val flag: ImageView
        val code: TextView
        val name: TextView
        val rightIcon: ImageView

        init {
            view.apply {
                flag = findViewById(R.id.flag)
                code = findViewById(R.id.currency)
                name = findViewById(R.id.currency_detail)
                rightIcon = findViewById(R.id.right_icon)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.currency_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.setOnClickListener {
            onClickListener?.invoke(holder, position, data[position])
        }

        thread {
            val flag = SpinnerAdapter.getLag?.invoke(data[position].countryCode)
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                holder.flag.setImageDrawable(flag)
            }
        }

        holder.code.text = data[position].code.uppercase()
        holder.name.text = data[position].name
        holder.rightIcon.setImageResource(
            if (selectCurrencySet?.contains(data[position].code) == true) {
                R.drawable.outline_check_box_24
            } else {
                R.drawable.outline_check_box_outline_blank_24
            }
        )
    }

    override fun getItemCount(): Int = data.size
}