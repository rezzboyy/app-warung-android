package com.warung.app.presentation.pos.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.warung.app.AppWarungApplication
import com.warung.app.R
import com.warung.app.databinding.DialogCheckoutBinding
import com.warung.app.presentation.pos.PosViewModel
import com.warung.app.presentation.pos.PosViewModelFactory
import com.warung.app.utils.CashDenominationHelper
import com.warung.app.utils.Constants
import com.warung.app.utils.CurrencyFormatter

class CheckoutDialogFragment : DialogFragment() {

    private var _binding: DialogCheckoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PosViewModel by activityViewModels {
        val app = requireActivity().application as AppWarungApplication
        PosViewModelFactory(app.productRepository, app.transactionRepository)
    }

    private var selectedPaymentMethod = Constants.PAYMENT_METHOD_CASH
    private var grandTotal: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        grandTotal = viewModel.cartState.value.grandTotal
        binding.tvCheckoutTotal.text = CurrencyFormatter.formatRupiah(grandTotal)

        setupPaymentMethods()
        setupQuickCashButtons()
        setupListeners()

        // Set default exact amount
        setReceivedAmount(grandTotal)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupPaymentMethods() {
        binding.cgPaymentMethods.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chip_cash) -> {
                    selectedPaymentMethod = Constants.PAYMENT_METHOD_CASH
                    binding.layoutQuickCash.visibility = View.VISIBLE
                    binding.tilCashReceived.visibility = View.VISIBLE
                    binding.layoutChange.visibility = View.VISIBLE
                }
                checkedIds.contains(R.id.chip_qris) -> {
                    selectedPaymentMethod = Constants.PAYMENT_METHOD_QRIS
                    binding.layoutQuickCash.visibility = View.GONE
                    binding.tilCashReceived.visibility = View.VISIBLE
                    setReceivedAmount(grandTotal)
                    binding.layoutChange.visibility = View.GONE
                }
                checkedIds.contains(R.id.chip_debt) -> {
                    selectedPaymentMethod = Constants.PAYMENT_METHOD_DEBT
                    binding.layoutQuickCash.visibility = View.GONE
                    binding.tilCashReceived.visibility = View.GONE
                    binding.layoutChange.visibility = View.GONE
                }
            }
        }
    }

    private fun setupQuickCashButtons() {
        binding.btnExactMoney.setOnClickListener { setReceivedAmount(grandTotal) }

        val denominations = CashDenominationHelper.calculateSmartDenominations(grandTotal)
        binding.btnCash10k.text = CashDenominationHelper.formatDenominationLabel(denominations[0])
        binding.btnCash10k.setOnClickListener { setReceivedAmount(denominations[0]) }

        binding.btnCash20k.text = CashDenominationHelper.formatDenominationLabel(denominations[1])
        binding.btnCash20k.setOnClickListener { setReceivedAmount(denominations[1]) }

        binding.btnCash50k.text = CashDenominationHelper.formatDenominationLabel(denominations[2])
        binding.btnCash50k.setOnClickListener { setReceivedAmount(denominations[2]) }

        binding.btnCash100k.text = CashDenominationHelper.formatDenominationLabel(denominations[3])
        binding.btnCash100k.setOnClickListener { setReceivedAmount(denominations[3]) }
    }

    private fun setReceivedAmount(amount: Double) {
        binding.etCashReceived.setText(amount.toLong().toString())
        binding.etCashReceived.setSelection(binding.etCashReceived.text?.length ?: 0)
    }

    private fun setupListeners() {
        binding.etCashReceived.doAfterTextChanged { text ->
            calculateChange(text?.toString()?.toDoubleOrNull() ?: 0.0)
        }

        binding.btnCheckoutCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCheckoutConfirm.setOnClickListener {
            processPayment()
        }
    }

    private fun calculateChange(cashReceived: Double) {
        val diff = cashReceived - grandTotal
        if (diff >= 0) {
            binding.tvChangeLabel.text = "Kembalian:"
            binding.tvChangeAmount.text = CurrencyFormatter.formatRupiah(diff)
            binding.tvChangeAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))
            binding.tilCashReceived.error = null
        } else {
            binding.tvChangeLabel.text = "Uang Kurang:"
            binding.tvChangeAmount.text = CurrencyFormatter.formatRupiah(-diff)
            binding.tvChangeAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_danger))
        }
    }

    private fun processPayment() {
        val cashReceived = if (selectedPaymentMethod == Constants.PAYMENT_METHOD_DEBT) {
            0.0
        } else {
            binding.etCashReceived.text?.toString()?.toDoubleOrNull() ?: 0.0
        }

        if (selectedPaymentMethod != Constants.PAYMENT_METHOD_DEBT && cashReceived < grandTotal) {
            binding.tilCashReceived.error = "Uang yang diterima kurang dari total tagihan!"
            return
        }

        viewModel.checkout(
            paymentMethod = selectedPaymentMethod,
            paidAmount = cashReceived
        )
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CheckoutDialog"

        fun newInstance() = CheckoutDialogFragment()
    }
}
