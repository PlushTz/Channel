package com.eetrust.channel

/**
 * Desc:
 * @author lijt
 * Created on 2024/1/24
 * Email: lijt@eetrust.com
 */
interface OnDragListener {
    /**
     * Item被选中时触发
     */
    fun onItemSelected()


    /**
     * Item在拖拽结束/滑动结束后触发
     */
    fun onItemFinish()
}