package com.warung.app.presentation.report.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.warung.app.data.local.entity.CashFlowEntity
import com.warung.app.databinding.ItemExpenseBinding
import com.warung.app.utils.CurrencyFormatter
import com.warung.app.utils.DateFormatter

class ExpenseAdapter : ListAdapter<CashFlowEntity, ExpenseAdapter.ExpenseViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CashFlowEntity) {
            binding.tvExpenseCategory.text = item.category
            binding.tvExpenseDescription.text = item.description
            binding.tvExpenseDate.text = DateFormatter.formatDateTime(item.date)
            binding.tvExpenseAmount.text = "- ${CurrencyFormatter.formatRupiah(item.amount)}"
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CashFlowEntity>() {
        override fun areItemsTheSame(oldItem: CashFlowEntity, newItem: CashFlowEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CashFlowEntity, newItem: CashFlowEntity): Boolean {
            return oldItem == newItem
        }
    }
}
