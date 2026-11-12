package com.warung.app.presentation.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.warung.app.AppWarungApplication
import com.warung.app.data.local.entity.CategoryEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.databinding.FragmentProductListBinding
import com.warung.app.presentation.product.adapter.ProductListAdapter
import com.warung.app.utils.Resource
import kotlinx.coroutines.launch

class ProductListFragment : Fragment() {

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductViewModel by activityViewModels {
        ProductViewModelFactory((requireActivity().application as AppWarungApplication).productRepository)
    }

    private lateinit var productAdapter: ProductListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductListAdapter(
            onEditClick = { product ->
                showAddEditProductDialog(product)
            },
            onDeleteClick = { product ->
                confirmDeleteProduct(product)
            }
        )

        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearchProduct.doAfterTextChanged { text ->
            viewModel.searchProducts(text?.toString() ?: "")
        }
    }

    private fun setupListeners() {
        binding.fabAddProduct.setOnClickListener {
            showAddEditProductDialog(null)
        }
    }

    private fun showAddEditProductDialog(product: ProductEntity?) {
        val dialog = AddEditProductDialogFragment.newInstance(product)
        dialog.show(childFragmentManager, AddEditProductDialogFragment.TAG)
    }

    private fun confirmDeleteProduct(product: ProductEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus Produk")
            .setMessage("Apakah Anda yakin ingin menghapus '${product.name}'?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteProduct(product)
            }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.productsState.collect { state ->
                        handleProductState(state)
                    }
                }

                launch {
                    viewModel.categories.collect { categories ->
                        setupCategoryChips(categories)
                        productAdapter.setCategories(categories)
                    }
                }

                launch {
                    viewModel.userMessage.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun handleProductState(state: Resource<List<ProductEntity>>) {
        when (state) {
            is Resource.Loading -> {
                binding.pbLoading.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
            }
            is Resource.Success -> {
                binding.pbLoading.visibility = View.GONE
                productAdapter.submitList(state.data)

                if (state.data.isEmpty()) {
                    binding.rvProducts.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvProducts.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                }
            }
            is Resource.Error -> {
                binding.pbLoading.visibility = View.GONE
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCategoryChips(categories: List<CategoryEntity>) {
        binding.cgCategories.removeAllViews()

        // "Semua" chip
        val allChip = Chip(requireContext()).apply {
            text = "Semua"
            isCheckable = true
            isChecked = (viewModel.selectedCategoryId.value == null)
            setOnClickListener {
                viewModel.selectCategory(null)
            }
        }
        binding.cgCategories.addView(allChip)

        // Category chips
        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category.name
                isCheckable = true
                isChecked = (viewModel.selectedCategoryId.value == category.id)
                setOnClickListener {
                    viewModel.selectCategory(category.id)
                }
            }
            binding.cgCategories.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ProductListFragment()
    }
}
