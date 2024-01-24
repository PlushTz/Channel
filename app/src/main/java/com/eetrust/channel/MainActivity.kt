package com.eetrust.channel

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.eetrust.channel.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val channelAdapter = ChannelAdapter()
    private val gridLayoutManager = GridLayoutManager(this@MainActivity, 3)
    private val addedChannels = mutableListOf<Channel>()
    private val notAddedChannels = mutableListOf<Channel>()
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
    }

    private fun initData() {
        for (i in 0 until 10) {
            addedChannels.add(Channel(i.toLong(), "已添加$i"))
            notAddedChannels.add(Channel(i.toLong(), "未添加$i"))
        }
    }

    private fun initView() {
        val callback = ItemTouchHelperCallback()
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
        binding.recyclerView.adapter = channelAdapter
        binding.recyclerView.layoutManager = gridLayoutManager
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val viewType = channelAdapter.getItemViewType(position)
                return if (viewType == ChannelAdapter.TYPE_ADDED_CHANNEL || viewType == ChannelAdapter.TYPE_NOT_ADDED_CHANNEL) 1 else 3;
            }
        }
        channelAdapter.setItemTouchHelper(itemTouchHelper)
        channelAdapter.setOnChannelItemClickListener(object : ChannelAdapter.OnChannelItemClickListener {
            override fun onItemClick(view: View, position: Int, channel: Channel) {
                Toast.makeText(this@MainActivity, channel.name, Toast.LENGTH_SHORT)
                    .show()
            }
        })
        channelAdapter.initAddedChannels(addedChannels)
        channelAdapter.initNotAddedChannels(notAddedChannels)
        channelAdapter.setOnChannelDoneListener(object : ChannelAdapter.OnChannelDoneListener {
            override fun onChannelDone(channels: List<Channel>) {
                Toast.makeText(this@MainActivity, "完成", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }
}