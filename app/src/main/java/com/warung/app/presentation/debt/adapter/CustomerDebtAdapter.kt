package com.warung.app.presentation.debt.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.warung.app.R
import com.warung.app.data.local.entity.CustomerEntity
import com.warung.app.databinding.ItemCustomerDebtBinding
import com.warung.app.utils.CurrencyFormatter

class CustomerDebtAdapter(
    private val onPayClick: (CustomerEntity) -> Unit,
    private val onAddDebtClick: (CustomerEntity) -> Unit,
    private val onWhatsAppClick: (CustomerEntity) -> Unit
) : ListAdapter<CustomerEntity, CustomerDebtAdapter.CustomerViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerDebtBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CustomerViewHolder(
        private val binding: ItemCustomerDebtBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: CustomerEntity) {
            val context = itemView.context

            binding.tvCustomerName.text = customer.name
            binding.tvCustomerPhone.text = customer.phoneNumber ?: "Tidak ada nomor HP"

            if (!customer.address.isNullOrBlank()) {
                binding.tvCustomerAddress.visibility = View.VISIBLE
                binding.tvCustomerAddress.text = "Alamat: ${customer.address}"
            } else {
                binding.tvCustomerAddress.visibility = View.GONE
            }

            if (customer.totalDebt > 0.0) {
                binding.tvCustomerDebt.text = CurrencyFormatter.formatRupiah(customer.totalDebt)
                binding.tvCustomerDebt.setBackgroundResource(R.drawable.bg_badge_low_stock)
                binding.tvCustomerDebt.setTextColor(ContextCompat.getColor(context, R.color.color_danger))
                binding.btnPayDebt.visibility = View.VISIBLE

                if (!customer.phoneNumber.isNullOrBlank()) {
                    binding.btnWhatsappReminder.visibility = View.VISIBLE
                } else {
                    binding.btnWhatsappReminder.visibility = View.GONE
                }
            } else {
                binding.tvCustomerDebt.text = "Lunas"
                binding.tvCustomerDebt.setBackgroundResource(R.drawable.bg_badge_normal_stock)
                binding.tvCustomerDebt.setTextColor(ContextCompat.getColor(context, R.color.color_success))
                binding.btnPayDebt.visibility = View.GONE
                binding.btnWhatsappReminder.visibility = View.GONE
            }

            binding.btnPayDebt.setOnClickListener {
                onPayClick(customer)
            }

            binding.btnAddDebt.setOnClickListener {
                onAddDebtClick(customer)
            }

            binding.btnWhatsappReminder.setOnClickListener {
                onWhatsAppClick(customer)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CustomerEntity>() {
        override fun areItemsTheSame(oldItem: CustomerEntity, newItem: CustomerEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CustomerEntity, newItem: CustomerEntity): Boolean {
            return oldItem == newItem
        }
    }
}
