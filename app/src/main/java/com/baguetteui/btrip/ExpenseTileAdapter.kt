package com.baguetteui.btrip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class ExpenseTileAdapter(
    private val items: MutableList<Expense>,
    private val onClick: (Expense) -> Unit,
    private val onLongPress: (Expense, View) -> Unit
) : RecyclerView.Adapter<ExpenseTileAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvPay: TextView = itemView.findViewById(R.id.tvPay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_expense_tile, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.tvTitle.text = e.title
        holder.tvAmount.text = formatCny(e.amountCny)
        holder.tvPay.text = e.paymentMethod.displayName
        holder.itemView.setOnClickListener { onClick(e) }
        holder.itemView.setOnLongClickListener {
            onLongPress(e, it)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    fun submit() {
        notifyDataSetChanged()
    }

    private fun formatCny(amount: Int): String {
        val nf = NumberFormat.getNumberInstance(Locale.CHINA)
        return "¥${nf.format(amount)}"
    }
}