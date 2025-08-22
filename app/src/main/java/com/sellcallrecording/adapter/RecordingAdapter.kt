package com.sellcallrecording.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sellcallrecording.database.Recording
import com.sellcallrecording.databinding.PendingRecordinglistBinding
import com.sellcallrecording.util.ClickListener

class RecordingAdapter(
    private val context: Context,
    private var recordings: MutableList<Recording>,
    private val listener: ClickListener
) : RecyclerView.Adapter<RecordingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PendingRecordinglistBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    fun updateRecordings(newRecordings: MutableList<Recording>) {
        recordings.clear()
        recordings.addAll(newRecordings)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = recordings[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = recordings.size

    inner class ViewHolder(private val binding: PendingRecordinglistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Recording) {
            binding.text1.text = item.fileName
            binding.root.setOnClickListener {
                listener.onItemSelected(adapterPosition, item)
            }
        }
    }
}
