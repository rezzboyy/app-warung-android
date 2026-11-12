package com.warung.app.presentation.pos

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
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.warung.app.AppWarungApplication
import com.warung.app.data.local.entity.CategoryEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.databinding.FragmentPosBinding
import com.warung.app.presentation.pos.adapter.PosProductAdapter
import com.warung.app.presentation.pos.dialog.CartBottomSheet
import com.warung.app.presentation.pos.dialog.CheckoutDialogFragment
import com.warung.app.presentation.pos.dialog.ReceiptDialogFragment
import com.warung.app.utils.CurrencyFormatter
import com.warung.app.utils.Resource
import kotlinx.coroutines.launch

class PosFragment : Fragment() {

    private var _binding: FragmentPosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PosViewModel by activityViewModels {
        val app = requireActivity().application as AppWarungApplication
        PosViewModelFactory(app.productRepository, app.transactionRepository)
    }

    private lateinit var posAdapter: PosProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPosBinding.inflate(inflater, container, false)
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
        posAdapter = PosProductAdapter(
            onProductClick = { product ->
                viewModel.addToCart(product)
            }
        )

        binding.rvPosProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = posAdapter
        }
    }

    private fun setupSearch() {
        binding.etPosSearch.doAfterTextChanged { text ->
            viewModel.search(text?.toString() ?: "")
        }
    }

    private fun setupListeners() {
        binding.cardBottomCart.setOnClickListener {
            openCartBottomSheet()
        }

        binding.btnBarViewCart.setOnClickListener {
            openCartBottomSheet()
        }

        binding.btnScanBarcode.setOnClickListener {
            com.warung.app.presentation.pos.dialog.BarcodeScannerDialogFragment.newInstance()
                .show(childFragmentManager, com.warung.app.presentation.pos.dialog.BarcodeScannerDialogFragment.TAG)
        }
    }

    private fun openCartBottomSheet() {
        val cartSheet = CartBottomSheet.newInstance {
            openCheckoutDialog()
        }
        cartSheet.show(childFragmentManager, CartBottomSheet.TAG)
    }

    private fun openCheckoutDialog() {
        val checkoutDialog = CheckoutDialogFragment.newInstance()
        checkoutDialog.show(childFragmentManager, CheckoutDialogFragment.TAG)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.productsState.collect { state ->
                        handleProductsState(state)
                    }
                }

                launch {
                    viewModel.categories.collect { categories ->
                        setupCategoryChips(categories)
                        posAdapter.setCategories(categories)
                    }
                }

                launch {
                    viewModel.cartState.collect { cart ->
                        posAdapter.setCartState(cart)

                        if (cart.isEmpty) {
                            binding.cardBottomCart.visibility = View.GONE
                        } else {
                            binding.cardBottomCart.visibility = View.VISIBLE
                            binding.tvBarCartItemsCount.text = "${cart.totalQuantity} Item Belanja"
                            binding.tvBarCartTotal.text = CurrencyFormatter.formatRupiah(cart.grandTotal)
                        }
                    }
                }

                launch {
                    viewModel.posEvent.collect { event ->
                        when (event) {
                            is PosEvent.ShowToast -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                            is PosEvent.OpenCheckoutDialog -> {
                                openCheckoutDialog()
                            }
                            is PosEvent.TransactionSuccess -> {
                                val receiptDialog = ReceiptDialogFragment.newInstance(event.transaction, event.items)
                                receiptDialog.show(childFragmentManager, ReceiptDialogFragment.TAG)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleProductsState(state: Resource<List<ProductEntity>>) {
        when (state) {
            is Resource.Loading -> {
                binding.pbPosLoading.visibility = View.VISIBLE
                binding.layoutPosEmpty.visibility = View.GONE
            }
            is Resource.Success -> {
                binding.pbPosLoading.visibility = View.GONE
                posAdapter.submitList(state.data)

                if (state.data.isEmpty()) {
                    binding.rvPosProducts.visibility = View.GONE
                    binding.layoutPosEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvPosProducts.visibility = View.VISIBLE
                    binding.layoutPosEmpty.visibility = View.GONE
                }
            }
            is Resource.Error -> {
                binding.pbPosLoading.visibility = View.GONE
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCategoryChips(categories: List<CategoryEntity>) {
        binding.cgPosCategories.removeAllViews()

        val allChip = Chip(requireContext()).apply {
            text = "Semua"
            isCheckable = true
            isChecked = (viewModel.selectedCategoryId.value == null)
            setOnClickListener {
                viewModel.selectCategory(null)
            }
        }
        binding.cgPosCategories.addView(allChip)

        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category.name
                isCheckable = true
                isChecked = (viewModel.selectedCategoryId.value == category.id)
                setOnClickListener {
                    viewModel.selectCategory(category.id)
                }
            }
            binding.cgPosCategories.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = PosFragment()
    }
}
