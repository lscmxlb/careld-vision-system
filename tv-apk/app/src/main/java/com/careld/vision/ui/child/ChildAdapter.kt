package com.careld.vision.ui.child

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.careld.vision.R

/**
 * Child list adapter
 */
class ChildAdapter(
    private val onChildClick: (ChildSearchViewModel.ChildDisplay) -> Unit
) : ListAdapter<ChildSearchViewModel.ChildDisplay, ChildAdapter.ChildViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_child, parent, false)
        return ChildViewHolder(view, onChildClick)
    }

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChildViewHolder(
        itemView: View,
        private val onChildClick: (ChildSearchViewModel.ChildDisplay) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvInfo: TextView = itemView.findViewById(R.id.tvInfo)

        fun bind(child: ChildSearchViewModel.ChildDisplay) {
            tvName.text = child.name
            
            val infoText = buildString {
                append("${child.gender}")
                if (child.age > 0) {
                    append(" | ${child.age}岁")
                }
                if (!child.phone.isNullOrEmpty()) {
                    append(" | ${child.phone}")
                }
            }
            tvInfo.text = infoText
            
            itemView.setOnClickListener { onChildClick(child) }
            
            // TV focus handling
            itemView.isFocusable = true
            itemView.isFocusableInTouchMode = true
            itemView.setOnFocusChangeListener { view, hasFocus ->
                view.isSelected = hasFocus
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChildSearchViewModel.ChildDisplay>() {
        override fun areItemsTheSame(
            oldItem: ChildSearchViewModel.ChildDisplay,
            newItem: ChildSearchViewModel.ChildDisplay
        ): Boolean {
            return oldItem.childId == newItem.childId
        }

        override fun areContentsTheSame(
            oldItem: ChildSearchViewModel.ChildDisplay,
            newItem: ChildSearchViewModel.ChildDisplay
        ): Boolean {
            return oldItem == newItem
        }
    }
}
