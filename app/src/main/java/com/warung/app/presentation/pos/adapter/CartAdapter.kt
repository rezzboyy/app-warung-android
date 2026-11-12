package com.warung.app.presentation.pos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.warung.app.databinding.ItemCartItemBinding
import com.warung.app.domain.model.CartItem
import com.warung.app.utils.CurrencyFormatter

class CartAdapter(
    private val onPlusClick: (CartItem) -> Unit,
    private val onMinusClick: (CartItem) -> Unit,
    private val onRemoveClick: (CartItem) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(
        private val binding: ItemCartItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            binding.tvCartProductName.text = item.product.name
            binding.tvCartUnitPrice.text = "@ ${CurrencyFormatter.formatRupiah(item.product.sellPrice)} / ${item.product.unit}"
            binding.tvCartItemSubtotal.text = CurrencyFormatter.formatRupiah(item.subtotal)
            binding.tvCartQuantity.text = item.quantity.toString()

            binding.btnCartPlus.setOnClickListener {
                onPlusClick(item)
            }

            binding.btnCartMinus.setOnClickListener {
                onMinusClick(item)
            }

            binding.btnCartRemove.setOnClickListener {
                onRemoveClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.quantity == newItem.quantity &&
                   oldItem.subtotal == newItem.subtotal
        }
    }
}
