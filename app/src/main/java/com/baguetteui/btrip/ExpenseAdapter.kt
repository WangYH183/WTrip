package com.baguetteui.btrip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExpenseAdapter(
    private val items: MutableList<Expense>,
    private val onClick: (Expense) -> Unit,
    private val onLongPress: (Expense, View) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
    }

    fun submit() = notifyDataSetChanged()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.tvTitle.text = e.title
        holder.tvAmount.text = "¥${e.amountCny}"
        holder.tvCategory.text = "${e.mainType.displayName} · ${e.paymentMethod.displayName}"

        holder.itemView.setOnClickListener { onClick(e) }
        holder.itemView.setOnLongClickListener {
            onLongPress(e, it)
            true
        }
    }

    override fun getItemCount(): Int = items.size
}