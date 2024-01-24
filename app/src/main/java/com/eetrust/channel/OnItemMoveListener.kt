package com.eetrust.channel

/**
 * Desc:
 * @author lijt
 * Created on 2024/1/23
 * Email: lijt@eetrust.com
 */
interface OnItemMoveListener {
    fun onItemMove(fromPosition: Int, toPosition: Int)
    fun onChangeItem()
}