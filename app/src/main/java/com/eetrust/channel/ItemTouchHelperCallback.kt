package com.eetrust.channel

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * Desc:
 * @author lijt
 * Created on 2024/1/24
 * Email: lijt@eetrust.com
 */
class ItemTouchHelperCallback : ItemTouchHelper.Callback() {
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags: Int
        val manager = recyclerView.layoutManager
        dragFlags = if (manager is GridLayoutManager || manager is StaggeredGridLayoutManager) {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        } else {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
        }
        // 如果想支持滑动(删除)操作, swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END
        val swipeFlags = 0
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        // 不同Type之间不可移动
        if (viewHolder.itemViewType != target.itemViewType) {
            if (recyclerView.adapter is OnItemMoveListener && target.itemViewType == ChannelAdapter.TYPE_NOT_ADDED_CHANNEL) {
                val listener = recyclerView.adapter as OnItemMoveListener?
                listener?.onChangeItem()
            }
            return false
        }

        if (recyclerView.adapter is OnItemMoveListener) {
            val listener = recyclerView.adapter as OnItemMoveListener?
            listener?.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        }
        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        // 不在闲置状态
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder is OnDragListener) {
                viewHolder.onItemSelected()
            }
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (viewHolder is OnDragListener) {
            viewHolder.onItemFinish()
        }
        super.clearView(recyclerView, viewHolder)
    }

    // 不支持长按拖拽功能 手动控制
    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    // 不支持滑动功能
    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

    }

}