package com.sellcallrecording.loadmore

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class RecyclerViewLoadMoreScroll(private val layoutManager: StaggeredGridLayoutManager) :
    RecyclerView.OnScrollListener() {

    private var visibleThreshold = 10
    private var mOnLoadMoreListener: OnLoadMoreListener? = null
    private var isLoading = false
    private var lastVisibleItem = 0
    private var totalItemCount = 0

    init {
        visibleThreshold *= layoutManager.spanCount
    }

    fun setLoaded() {
        isLoading = false
    }

    fun getLoaded(): Boolean {
        return isLoading
    }

    fun setOnLoadMoreListener(onLoadMoreListener: OnLoadMoreListener?) {
        mOnLoadMoreListener = onLoadMoreListener
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (dy <= 0) return

        totalItemCount = layoutManager.itemCount

        if (layoutManager is StaggeredGridLayoutManager) {
            val lastVisibleItemPositions =
                layoutManager.findLastCompletelyVisibleItemPositions(null)
            lastVisibleItem = getLastVisibleItem(lastVisibleItemPositions)
        }

        if (!isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
            mOnLoadMoreListener?.onLoadMore()
            isLoading = true
        }
    }

    private fun getLastVisibleItem(lastVisibleItemPositions: IntArray): Int {
        var maxSize = 0
        for (i in lastVisibleItemPositions.indices) {
            if (i == 0) {
                maxSize = lastVisibleItemPositions[i]
            } else if (lastVisibleItemPositions[i] > maxSize) {
                maxSize = lastVisibleItemPositions[i]
            }
        }
        return maxSize
    }


}
