package com.warung.app.presentation.pos.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.warung.app.AppWarungApplication
import com.warung.app.databinding.BottomSheetCartBinding
import com.warung.app.presentation.pos.PosViewModel
import com.warung.app.presentation.pos.PosViewModelFactory
import com.warung.app.presentation.pos.adapter.CartAdapter
import com.warung.app.utils.CurrencyFormatter
import kotlinx.coroutines.launch

class CartBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCartBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PosViewModel by activityViewModels {
        val app = requireActivity().application as AppWarungApplication
        PosViewModelFactory(app.productRepository, app.transactionRepository)
    }

    private lateinit var cartAdapter: CartAdapter
    var onProceedCheckout: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeCart()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onPlusClick = { item ->
                viewModel.addToCart(item.product)
            },
            onMinusClick = { item ->
                viewModel.decreaseCartItem(item.product.id)
            },
            onRemoveClick = { item ->
                viewModel.removeCartItem(item.product.id)
            }
        )

        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    private fun setupListeners() {
        binding.btnClearCart.setOnClickListener {
            viewModel.clearCart()
            dismiss()
        }

        binding.btnProceedCheckout.setOnClickListener {
            dismiss()
            onProceedCheckout?.invoke()
        }
    }

    private fun observeCart() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cartState.collect { cart ->
                    if (cart.isEmpty) {
                        dismiss()
                    } else {
                        cartAdapter.submitList(cart.items)
                        binding.tvCartSubtotal.text = CurrencyFormatter.formatRupiah(cart.subtotal)
                        binding.tvCartGrandTotal.text = CurrencyFormatter.formatRupiah(cart.grandTotal)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CartBottomSheet"

        fun newInstance(onCheckout: () -> Unit): CartBottomSheet {
            return CartBottomSheet().apply {
                this.onProceedCheckout = onCheckout
            }
        }
    }
}
