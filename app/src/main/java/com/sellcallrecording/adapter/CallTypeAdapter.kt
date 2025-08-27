package com.sellcallrecording.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sellcallrecording.data.model.Category
import com.sellcallrecording.databinding.ItemCalltypeBinding
import com.sellcallrecording.util.ClickListener


class CallTypeAdapter(
    private val items: List<Category>,
    private val type: String?,
    private val listener: ClickListener
) :
    RecyclerView.Adapter<CallTypeAdapter.CallTypeViewHolder>() {

    inner class CallTypeViewHolder(private val binding: ItemCalltypeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Category) {
            binding.customView.setText(item.tcnt)
            binding.customView.setSelect(type == item.category_id)
            binding.itemText.text = item.category
            binding.itemText.isSelected = type == item.category_id
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