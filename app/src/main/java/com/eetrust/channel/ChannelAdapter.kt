package com.eetrust.channel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.MotionEventCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.eetrust.channel.databinding.ItemAddedChannelBinding
import com.eetrust.channel.databinding.ItemAddedChannelHeaderBinding
import com.eetrust.channel.databinding.ItemNotAddedChannelBinding
import com.eetrust.channel.databinding.ItemNotAddedChannelHeaderBinding

/**
 * Desc:频道管理适配器
 * @author lijt
 * Created on 2024/1/23
 * Email: lijt@eetrust.com
 */
class ChannelAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), OnItemMoveListener {
    companion object {
        const val TYPE_ADDED_CHANNEL_HEAD = 0 // Channel的头部
        const val TYPE_ADDED_CHANNEL = 1 // 已添加的Channel
        const val TYPE_NOT_ADDED_CHANNEL_HEAD = 2 // 未添加的Channel的头部
        const val TYPE_NOT_ADDED_CHANNEL = 3 // 未添加的Channel
        const val COUNT_ADDED_CHANNEL = 1 // 添加的header数量  该demo中 即标题部分 为 1
        const val COUNT_NOT_ADDED_CHANNEL = COUNT_ADDED_CHANNEL + 1 // 未添加之前的header数量  该demo中 即标题部分 为 COUNT_ADDED_CHANNEL + 1
        const val ANIM_TIME = 360L
        const val SPACE_TIME = 100
    }

    private val delayHandler = Handler(Looper.getMainLooper())

    // 已添加的Channel
    val addedChannels = mutableListOf<Channel>()

    // 未添加的Channel
    private val notAddedChannels = mutableListOf<Channel>()

    private var parentView: ViewGroup? = null

    // 是否处于编辑状态
    private var isEditMode: Boolean = false

    private var startTime: Long = 0

    private var mChannelItemClickListener: OnChannelItemClickListener? = null

    private var mOnChannelDoneListener: OnChannelDoneListener? = null

    private var mItemTouchHelper: ItemTouchHelper? = null

    private var mCurrentView: AddedChannelViewHolder? = null

    fun initAddedChannels(channels: List<Channel>) {
        addedChannels.clear()
        addedChannels.addAll(channels)
    }

    fun initNotAddedChannels(channels: List<Channel>) {
        this.notAddedChannels.clear()
        this.notAddedChannels.addAll(channels)
    }

    fun setItemTouchHelper(itemTouchHelper: ItemTouchHelper) {
        this.mItemTouchHelper = itemTouchHelper
    }

    fun setOnChannelItemClickListener(listener: OnChannelItemClickListener) {
        this.mChannelItemClickListener = listener
    }

    fun setOnChannelDoneListener(listener: OnChannelDoneListener) {
        this.mOnChannelDoneListener = listener
    }

    fun reset() {
        if (isEditMode) {
            isEditMode = false
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 -> {
                TYPE_ADDED_CHANNEL_HEAD
            }

            position == addedChannels.size + 1 -> {
                TYPE_NOT_ADDED_CHANNEL_HEAD
            }

            position > 0 && position < addedChannels.size + 1 -> {
                TYPE_ADDED_CHANNEL
            }

            else -> {
                TYPE_NOT_ADDED_CHANNEL
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        this.parentView = parent
        return when (viewType) {
            TYPE_ADDED_CHANNEL_HEAD -> {
                AddedChannelHeaderViewHolder(ItemAddedChannelHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            TYPE_ADDED_CHANNEL -> {
                AddedChannelViewHolder(ItemAddedChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false), parent as RecyclerView)
            }

            TYPE_NOT_ADDED_CHANNEL_HEAD -> {
                NotAddedChannelHeaderViewHolder(ItemNotAddedChannelHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }

            TYPE_NOT_ADDED_CHANNEL -> {
                NotAddedChannelViewHolder(ItemNotAddedChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false), parent as RecyclerView)
            }

            else -> {
                object : RecyclerView.ViewHolder(parent) {}
            }
        }
    }

    override fun getItemCount(): Int = addedChannels.size + notAddedChannels.size + COUNT_NOT_ADDED_CHANNEL

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AddedChannelHeaderViewHolder -> {
                holder.bind()
            }

            is AddedChannelViewHolder -> {
                holder.bind(holder, addedChannels[position - COUNT_ADDED_CHANNEL], position)
                holder.setIsRecyclable(false)
            }

            is NotAddedChannelHeaderViewHolder -> {}

            is NotAddedChannelViewHolder -> {
                holder.bind(holder, notAddedChannels[position - addedChannels.size - COUNT_NOT_ADDED_CHANNEL], position)
            }
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        val channel = addedChannels[fromPosition - COUNT_ADDED_CHANNEL]
        addedChannels.removeAt(fromPosition - COUNT_ADDED_CHANNEL)
        addedChannels.add(toPosition - COUNT_ADDED_CHANNEL, channel)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onChangeItem() {
        val position = mCurrentView?.adapterPosition ?: return
        val recyclerView = (parentView as RecyclerView)
        val targetView = recyclerView.layoutManager?.findViewByPosition(addedChannels.size + COUNT_NOT_ADDED_CHANNEL)
        val currentView = recyclerView.layoutManager?.findViewByPosition(position)
        // 如果targetView不在屏幕内,则indexOfChild为-1  此时不需要添加动画,因为此时notifyItemMoved自带一个向目标移动的动画
        // 如果在屏幕内,则添加一个位移动画
        if (recyclerView.indexOfChild(targetView) >= 0) {
            val targetX: Int
            val targetY: Int
            val manager = recyclerView.layoutManager
            val spanCount = (manager as GridLayoutManager?)?.spanCount ?: return

            // 移动后 高度将变化 (我的频道Grid 最后一个item在新的一行第一个)
            if ((addedChannels.size - COUNT_ADDED_CHANNEL) % spanCount == 0) {
                val preTargetView = recyclerView.layoutManager!!.findViewByPosition(addedChannels.size + COUNT_NOT_ADDED_CHANNEL - 1)
                targetX = preTargetView?.left ?: return
                targetY = preTargetView.top
            } else {
                targetX = targetView?.left ?: return
                targetY = targetView.top
            }
            moveAddedToNotAdded(mCurrentView)
            val intX = targetX.toFloat()
                .toInt()
            val intY = targetY.toFloat()
                .toInt()
            startAnimation(recyclerView, currentView, intX, intY)
        } else {
            moveAddedToNotAdded(mCurrentView)
        }
    }

    inner class AddedChannelHeaderViewHolder(private val binding: ItemAddedChannelHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.editTipsText.isVisible = !isEditMode
            binding.tvBtnEdit.text = getEditText(binding.root.context, isEditMode)
            binding.tvBtnEdit.setOnClickListener {
                binding.tvBtnEdit.text = getEditText(binding.root.context, isEditMode)
                if (!isEditMode) {
                    startEditMode()
                } else {
                    doneEditMode()
                    mOnChannelDoneListener?.onChannelDone(addedChannels)
                }
            }
        }
    }

    inner class AddedChannelViewHolder(private val binding: ItemAddedChannelBinding, private val recyclerView: RecyclerView) : RecyclerView.ViewHolder(binding.root), OnDragListener {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(holder: AddedChannelViewHolder, channel: Channel, position: Int) {
            binding.imgEdit.isVisible = isEditMode
            binding.tvAddedChannelLabel.text = channel.name
            setShakeView(itemView, isEditMode)
            holder.itemView.setOnClickListener {
                if (isEditMode) {
                    val targetView = recyclerView.layoutManager?.findViewByPosition(addedChannels.size + COUNT_NOT_ADDED_CHANNEL)
                    // 如果targetView不在屏幕内,则indexOfChild为-1  此时不需要添加动画,因为此时notifyItemMoved自带一个向目标移动的动画
                    // 如果在屏幕内,则添加一个位移动画
                    if (recyclerView.indexOfChild(targetView) > 0) {
                        moveAddedToNotAdded(this)
                    } else {
                        moveAddedToNotAdded(this)
                    }
                } else {
                    mChannelItemClickListener?.onItemClick(it, position - COUNT_ADDED_CHANNEL, channel)
                }
            }
            holder.itemView.setOnLongClickListener {
                if (!isEditMode) {
                    startEditMode()
                    // header 按钮文字 改成 "完成"
                    val view = recyclerView.getChildAt(0)
                    if (view == recyclerView.layoutManager?.findViewByPosition(0)) {
                        view.findViewById<TextView>(R.id.tv_btn_edit).text = getEditText(holder.itemView.context, isEditMode)
                    }
                }
                true
            }
            holder.itemView.setOnTouchListener { _, event ->
                if (isEditMode) {
                    mCurrentView = this@AddedChannelViewHolder
                    when (MotionEventCompat.getActionMasked(event)) {
                        MotionEvent.ACTION_DOWN -> startTime = System.currentTimeMillis()
                        MotionEvent.ACTION_MOVE -> if (System.currentTimeMillis() - startTime > SPACE_TIME) {
                            mItemTouchHelper?.startDrag(this@AddedChannelViewHolder)
                        }

                        MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> startTime = 0
                    }
                }
                return@setOnTouchListener false
            }
            setShakeView(binding.myRelaItem, isEditMode)
        }

        override fun onItemSelected() {
            binding.tvAddedChannelLabel.setBackgroundResource(R.drawable.bg_channel_p)
        }

        override fun onItemFinish() {
            binding.tvAddedChannelLabel.setBackgroundResource(R.drawable.bg_channel)
        }
    }

    inner class NotAddedChannelHeaderViewHolder(binding: ItemNotAddedChannelHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    inner class NotAddedChannelViewHolder(private val binding: ItemNotAddedChannelBinding, private val recyclerView: RecyclerView) : RecyclerView.ViewHolder(binding.root) {
        fun bind(holder: NotAddedChannelViewHolder, channel: Channel, position: Int) {
            binding.imageAdd.isVisible = isEditMode
            binding.tvAddedChannelLabel.text = channel.name
            holder.itemView.setOnClickListener {
                if (!isEditMode) {
                    return@setOnClickListener
                }
                val manager = recyclerView.layoutManager
                // 如果RecyclerView滑动到底部,移动的目标位置的y轴 - height
                // 目标位置的前一个item  即当前MyChannel的最后一个
                val preTargetView = manager?.findViewByPosition(addedChannels.size - 1 + COUNT_ADDED_CHANNEL)

                // 如果targetView不在屏幕内,则为-1  此时不需要添加动画,因为此时notifyItemMoved自带一个向目标移动的动画
                // 如果在屏幕内,则添加一个位移动画
                if (recyclerView.indexOfChild(preTargetView) >= 0) {
                    val gridLayoutManager = manager as GridLayoutManager?
                    val targetPosition: Int = addedChannels.size - 1 + COUNT_NOT_ADDED_CHANNEL
                    val spanCount = gridLayoutManager?.spanCount!!
                    // 如果当前位置是otherChannel可见的最后一个
                    // 并且 当前位置不在grid的第一个位置
                    // 则 需要延迟250秒 notifyItemMove , 这是因为这种情况 , 并不触发ItemAnimator , 会直接刷新界面
                    // 导致我们的位移动画刚开始,就已经notify完毕,引起不同步问题
                    if (position == gridLayoutManager.findLastVisibleItemPosition() && (position - addedChannels.size - COUNT_NOT_ADDED_CHANNEL) % spanCount != 0 && (targetPosition - COUNT_ADDED_CHANNEL) % spanCount != 0) {
                        moveNotAddedToAddedByDelay(holder)
                    } else {
                        moveNotAddedToAdded(holder)
                    }
//                    startAnimation(recyclerView, currentView, targetX.toFloat().toInt(), targetY.toFloat().toInt())
                } else {
                    moveNotAddedToAdded(holder)
                }
            }
        }
    }

    private fun getEditText(context: Context, isEditMode: Boolean): String {
        return if (!isEditMode) {
            ContextCompat.getString(context, R.string.text_edit)
        } else {
            ContextCompat.getString(context, R.string.text_done)
        }
    }

    /**
     * 已添加 移动到 未添加
     *
     * @param holder:AddChannelViewHolder
     */
    private fun moveAddedToNotAdded(holder: AddedChannelViewHolder?) {
        if (addedChannels.size == 1) {
            return
        }
        val position: Int = holder?.adapterPosition ?: return
        val startPosition: Int = position - COUNT_ADDED_CHANNEL
        if (startPosition > addedChannels.size - 1) {
            return
        }
        val channel = addedChannels[startPosition]
        addedChannels.removeAt(startPosition)
        notAddedChannels.add(0, channel)
        notifyItemMoved(position, addedChannels.size + COUNT_NOT_ADDED_CHANNEL)
    }

    /**
     * 未添加 移动到 已添加
     * @param holder OtherChannelViewHolder
     */
    private fun moveNotAddedToAdded(holder: NotAddedChannelViewHolder) {
        val position = processItemRemoveAdd(holder)
        if (position == -1) {
            return
        }
        notifyItemMoved(position, addedChannels.size - 1 + COUNT_ADDED_CHANNEL)
    }

    /**
     * 未添加 移动到 已添加 伴随延迟
     * @param holder OtherChannelViewHolder
     */
    private fun moveNotAddedToAddedByDelay(holder: NotAddedChannelViewHolder) {
        val position: Int = processItemRemoveAdd(holder)
        if (position == -1) {
            return
        }
        delayHandler.postDelayed({ notifyItemMoved(position, addedChannels.size - 1 + COUNT_ADDED_CHANNEL) }, ANIM_TIME)
    }

    private fun processItemRemoveAdd(holder: NotAddedChannelViewHolder): Int {
        val position: Int = holder.adapterPosition
        val startPosition: Int = position - addedChannels.size - COUNT_NOT_ADDED_CHANNEL
        if (startPosition > notAddedChannels.size - 1) {
            return -1
        }
        val channel = notAddedChannels[startPosition]
        notAddedChannels.removeAt(startPosition)
        addedChannels.add(channel)
        return position
    }

    /**
     * 启动增删动画
     * @param recyclerView RecyclerView
     * @param currentView View
     * @param targetX Int
     * @param targetY Int
     */
    private fun startAnimation(recyclerView: RecyclerView, currentView: View?, targetX: Int?, targetY: Int?) {
        if (currentView == null || targetX == null || targetY == null) {
            return
        }
        val viewGroup = recyclerView.parent as ViewGroup
        val mirrorView: ImageView? = addMirrorView(viewGroup, recyclerView, currentView)
        val animation: Animation = getTranslateAnimator((targetX - currentView.left).toFloat(), (targetY - currentView.top).toFloat())
        currentView.visibility = View.INVISIBLE
        mirrorView?.startAnimation(animation)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                viewGroup.removeView(mirrorView)
                if (currentView.visibility == View.INVISIBLE) {
                    currentView.visibility = View.VISIBLE
                }
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    /**
     * 获取位移动画
     */
    private fun getTranslateAnimator(targetX: Float, targetY: Float): TranslateAnimation {
        val translateAnimation = TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.ABSOLUTE, targetX, Animation.RELATIVE_TO_SELF, 0f, Animation.ABSOLUTE, targetY)
        // RecyclerView默认移动动画250ms 这里设置360ms 是为了防止在位移动画结束后 remove(view)过早 导致闪烁
        translateAnimation.duration = ANIM_TIME
        translateAnimation.fillAfter = true
        return translateAnimation
    }

    /**
     * 添加需要移动的 镜像View
     * @param parent ViewGroup
     * @param recyclerView RecyclerView
     * @param view View
     * @return ImageView
     */
    private fun addMirrorView(parent: ViewGroup?, recyclerView: RecyclerView?, view: View?): ImageView? {
        /**
         * 我们要获取cache首先要通过setDrawingCacheEnable方法开启cache，然后再调用getDrawingCache方法就可以获得view的cache图片了。
         * buildDrawingCache方法可以不用调用，因为调用getDrawingCache方法时，若果cache没有建立，系统会自动调用buildDrawingCache方法生成cache。
         * 若想更新cache, 必须要调用destoryDrawingCache方法把旧的cache销毁，才能建立新的。
         * 当调用setDrawingCacheEnabled方法设置为false, 系统也会自动把原来的cache销毁。
         */
        if (parent == null || recyclerView == null || view == null) {
            return null
        }
        view.destroyDrawingCache()
        view.isDrawingCacheEnabled = true
        val mirrorView = ImageView(recyclerView.context)
        val bitmap = Bitmap.createBitmap(view.drawingCache)
        mirrorView.setImageBitmap(bitmap)
        view.isDrawingCacheEnabled = false
        val locations = IntArray(2)
        view.getLocationOnScreen(locations)
        val parenLocations = IntArray(2)
        recyclerView.getLocationOnScreen(parenLocations)
        val params = FrameLayout.LayoutParams(bitmap.width, bitmap.height)
        params.setMargins(locations[0], locations[1] - parenLocations[1], 0, 0)
        parent.addView(mirrorView, params)
        return mirrorView
    }

    /**
     * 开启编辑模式
     */
    private fun startEditMode() {
        isEditMode = true
        notifyDataSetChanged()
    }

    /**
     * 完成编辑模式
     */
    private fun doneEditMode() {
        isEditMode = false
        notifyDataSetChanged()
    }

    /**
     *  设置View抖动效果
     * @param view View?
     * @param isShake Boolean
     */
    private fun setShakeView(view: View?, isShake: Boolean) {
        if (isShake) {
            if (view?.animation != null) {
                view.animation?.start()
            }
            val rotate = RotateAnimation(-1F, 1F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
            val linearInterpolator = LinearInterpolator()
            rotate.interpolator = linearInterpolator
            rotate.duration = 100 //设置动画持续时间
            rotate.repeatCount = -1 //设置重复次数
            rotate.fillAfter = false //动画执行完后是否停留在执行完的状态
            rotate.startOffset = 10 //执行前的等待时间
            rotate.repeatMode = Animation.REVERSE
            view?.startAnimation(rotate)
        } else {
            if (view?.animation != null) {
                view.animation?.cancel()
            }
            view?.clearAnimation()
        }
    }

    interface OnChannelItemClickListener {
        fun onItemClick(view: View, position: Int, channel: Channel)
    }

    interface OnChannelDoneListener {
        fun onChannelDone(channels: List<Channel>)
    }
}