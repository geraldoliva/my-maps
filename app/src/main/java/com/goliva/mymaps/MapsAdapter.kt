package com.goliva.mymaps

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.goliva.mymaps.models.UserMap

class MapsAdapter(private val context: Context, private val userMaps: List<UserMap>, private val onClickListener: OnClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    interface OnClickListener {
        fun onItemClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_user_map, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val userMap = userMaps[position]
        holder.itemView.setOnClickListener {
            onClickListener.onItemClick(position)
        }
        val texViewTitle = holder.itemView.findViewById<TextView>(R.id.tvMapTitle)
        texViewTitle.text = userMap.title
    }

    override fun getItemCount() = userMaps.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
