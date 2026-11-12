package com.warung.app.presentation.pos.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.warung.app.data.local.entity.CategoryEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.databinding.ItemPosProductBinding
import com.warung.app.domain.model.CartState
import com.warung.app.utils.CurrencyFormatter

class PosProductAdapter(
    private val onProductClick: (ProductEntity) -> Unit
) : ListAdapter<ProductEntity, PosProductAdapter.PosProductViewHolder>(DiffCallback) {

    private var categoryMap: Map<Long, String> = emptyMap()
    private var cartState: CartState = CartState()

    fun setCategories(categories: List<CategoryEntity>) {
        categoryMap = categories.associate { it.id to it.name }
        notifyDataSetChanged()
    }

    fun setCartState(state: CartState) {
        cartState = state
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosProductViewHolder {
        val binding = ItemPosProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PosProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PosProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PosProductViewHolder(
        private val binding: ItemPosProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity) {
            binding.tvPosProductName.text = product.name
            binding.tvPosProductPrice.text = CurrencyFormatter.formatRupiah(product.sellPrice)

            val catName = product.categoryId?.let { categoryMap[it] } ?: "Umum"
            binding.tvPosProductCategory.text = catName

            binding.tvPosProductStock.text = "Sisa: ${product.stock} ${product.unit}"

            // Check if product is in cart
            val cartItem = cartState.getItem(product.id)
            if (cartItem != null && cartItem.quantity > 0) {
                binding.tvInCartBadge.visibility = View.VISIBLE
                binding.tvInCartBadge.text = "x${cartItem.quantity}"
            } else {
                binding.tvInCartBadge.visibility = View.GONE
            }

            binding.cardPosProduct.setOnClickListener {
                onProductClick(product)
            }

            binding.btnAddToCart.setOnClickListener {
                onProductClick(product)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ProductEntity>() {
        override fun areItemsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem == newItem
        }
    }
}
