package com.baguetteui.btrip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TripAdapter(
    private val items: MutableList<Trip>,
    private val getSummary: (Trip) -> Pair<Int, Int?>,
    private val onClick: (Trip) -> Unit,
    private val onLongPress: (Trip, View) -> Unit
) : RecyclerView.Adapter<TripAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
    }

    fun submit() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = items[position]
        holder.tvTitle.text = t.title

        val (itemCount, totalCost) = getSummary(t)
        val costText = totalCost?.let { "¥$it" } ?: "—"
        holder.tvSubtitle.text = "共 $itemCount 项花费 · 预计总费用：$costText"

        holder.itemView.setOnClickListener { onClick(t) }
        holder.itemView.setOnLongClickListener {
            onLongPress(t, it)
            true
        }
    }

    override fun getItemCount(): Int = items.size
}