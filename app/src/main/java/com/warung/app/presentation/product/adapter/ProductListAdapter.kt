package com.warung.app.presentation.product.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.warung.app.R
import com.warung.app.data.local.entity.CategoryEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.databinding.ItemProductCardBinding
import com.warung.app.utils.CurrencyFormatter

class ProductListAdapter(
    private val onEditClick: (ProductEntity) -> Unit,
    private val onDeleteClick: (ProductEntity) -> Unit
) : ListAdapter<ProductEntity, ProductListAdapter.ProductViewHolder>(DiffCallback) {

    private var categoryMap: Map<Long, String> = emptyMap()

    fun setCategories(categories: List<CategoryEntity>) {
        categoryMap = categories.associate { it.id to it.name }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity) {
            val context = itemView.context

            binding.tvProductName.text = product.name

            val categoryName = product.categoryId?.let { categoryMap[it] } ?: "Umum"
            binding.tvProductCategory.text = "Kategori: $categoryName"

            binding.tvProductPrice.text = CurrencyFormatter.formatRupiah(product.sellPrice)
            binding.tvProductBuyPrice.text = "(Modal: ${CurrencyFormatter.formatRupiah(product.buyPrice)})"

            // Stock badge status
            if (product.stock <= 5) {
                binding.tvProductStock.text = "Stok Menipis: ${product.stock} ${product.unit}"
                binding.tvProductStock.setBackgroundResource(R.drawable.bg_badge_low_stock)
                binding.tvProductStock.setTextColor(ContextCompat.getColor(context, R.color.color_danger))
            } else {
                binding.tvProductStock.text = "Stok: ${product.stock} ${product.unit}"
                binding.tvProductStock.setBackgroundResource(R.drawable.bg_badge_normal_stock)
                binding.tvProductStock.setTextColor(ContextCompat.getColor(context, R.color.color_success))
            }

            binding.btnEditProduct.setOnClickListener {
                onEditClick(product)
            }

            binding.btnDeleteProduct.setOnClickListener {
                onDeleteClick(product)
            }

            binding.cardProduct.setOnClickListener {
                onEditClick(product)
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
