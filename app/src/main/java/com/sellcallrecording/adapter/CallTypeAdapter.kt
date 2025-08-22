package com.sellcallrecording.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sellcallrecording.data.model.CallType
import com.sellcallrecording.databinding.ItemCalltypeBinding
import com.sellcallrecording.util.ClickListener
import com.sellcallrecording.util.CustomView


class CallTypeAdapter(
    private val context: Context,
    private val items: List<CallType>,
    private val type: String?,
    private val listener: ClickListener
) :
    RecyclerView.Adapter<CallTypeAdapter.CallTypeViewHolder>() {

    inner class CallTypeViewHolder(private val binding: ItemCalltypeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CallType) {
            binding.customView.setText(item.tcnt)
            binding.customView.setSelect(type == item.id)
            binding.itemText.text = item.name
            binding.itemText.isSelected = type == item.id
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallTypeViewHolder {
        val binding = ItemCalltypeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CallTypeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CallTypeViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener { listener.onItemSelected(position, items[position]) }
    }

    override fun getItemCount(): Int = items.size
}