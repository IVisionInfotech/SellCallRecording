package com.sellcallrecording.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import com.sellcallrecording.R
import com.sellcallrecording.data.model.CallResponse
import com.sellcallrecording.databinding.DialogCallBinding
import com.sellcallrecording.databinding.ItemCallBinding
import com.sellcallrecording.databinding.ProgressbarItemBinding
import com.sellcallrecording.databinding.ShimmerframelayoutBinding
import com.sellcallrecording.util.ClickListener

class CallDataViewAdapter(
    private val context: Context,
    private val data: MutableList<CallResponse>,
    private val listener: ClickListener,
    private val listener2: ClickListener,
    private val listener3: ClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val originalData = mutableListOf<CallResponse>().apply { addAll(data) }
    private var isShimmering = false
    private var isLoadingAdded = false
    private var currentPopupWindow: PopupWindow? = null
    private var lastClickedItem: CallResponse? = null

    companion object {
        private const val ITEM = 0
        private const val LOADING = 1
        private const val SHIMMER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            isShimmering -> SHIMMER
            position == data.size - 1 && isLoadingAdded -> LOADING
            else -> ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            SHIMMER -> ShimmerViewHolder(ShimmerframelayoutBinding.inflate(inflater, parent, false))
            LOADING -> LoadingViewHolder(ProgressbarItemBinding.inflate(inflater, parent, false))
            else -> MyViewHolder(ItemCallBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ShimmerViewHolder -> holder.bind()
            is LoadingViewHolder -> { /* No specific binding needed */ }
            is MyViewHolder -> {
                val item = data[position]
                holder.bind(item)
                holder.layoutCall.setOnClickListener {
                    togglePopup(holder.itemView, item, position)
                }
                holder.detailImage.setOnClickListener { listener3.onItemSelected(position, item) }
            }
        }
    }

    override fun getItemCount() = if (isShimmering) 20 else data.size

    fun clearList() {
        data.clear()
        originalData.clear()
        notifyDataSetChanged()
    }

    fun showShimmer() {
        isShimmering = true
        notifyDataSetChanged()
    }

    fun hideShimmer() {
        isShimmering = false
        notifyDataSetChanged()
    }

    fun addData(newItems: List<CallResponse>) {
        hideShimmer()
        data.clear()
        originalData.clear()
        updateDataInternal(newItems)
    }

    fun updateData(newItems: List<CallResponse>) {
        val uniqueItems = newItems.filterNot { it in data }
        updateDataInternal(uniqueItems)
    }

    private fun updateDataInternal(newItems: List<CallResponse>) {
        data.addAll(newItems)
        originalData.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setLoading(loading: Boolean) {
        isLoadingAdded = loading
        notifyDataSetChanged()
    }

    fun searchData(query: String) {
        val filteredList = if (query.isEmpty()) originalData else originalData.filter { it.M_no.contains(query, ignoreCase = true) }
        updateFilteredData(filteredList)
    }

    private fun updateFilteredData(filteredList: List<CallResponse>) {
        data.clear()
        data.addAll(filteredList.distinctBy { it.M_no })
        notifyDataSetChanged()
    }

    private fun togglePopup(view: View, item: CallResponse, position: Int) {
        if (lastClickedItem == item) {
            currentPopupWindow?.dismiss()
            lastClickedItem = null
        } else {
            currentPopupWindow?.dismiss()
            showPopup(view, item, position)
            lastClickedItem = item
        }
    }

    private fun showPopup(view: View, item: CallResponse, position: Int) {
        val popupView = LayoutInflater.from(context).inflate(R.layout.dialog_call, null)
        val binding = DialogCallBinding.bind(popupView)

        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            isOutsideTouchable = true
            showAsDropDown(view, 0, 0)
        }

        binding.tvCAllPhone.text = item.M_no
        binding.tvWhatPhone.text = item.M_no

        binding.llCall.setOnClickListener {
            listener.onItemSelected(position, item)
            popupWindow.dismiss()
        }

        binding.llWhatsapp.setOnClickListener {
            listener2.onItemSelected(position, item)
            popupWindow.dismiss()
        }

        currentPopupWindow = popupWindow
    }

    inner class MyViewHolder(private val binding: ItemCallBinding) : RecyclerView.ViewHolder(binding.root) {
        val detailImage = binding.detailImage
        val layoutCall = binding.layoutCall

        fun bind(item: CallResponse) {
            binding.textViewName.text = item.M_no
            binding.textViewPhone.text = item.category
            binding.textViewTime.text = item.updated_date
        }
    }

    inner class ShimmerViewHolder(private val binding: ShimmerframelayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.shimmerLayout.startShimmer()
        }
    }

    inner class LoadingViewHolder(binding: ProgressbarItemBinding) : RecyclerView.ViewHolder(binding.root)

    fun convertSecToMin(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
}
