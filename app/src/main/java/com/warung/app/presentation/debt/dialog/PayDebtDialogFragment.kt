package com.warung.app.presentation.debt.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.warung.app.AppWarungApplication
import com.warung.app.data.local.entity.CustomerEntity
import com.warung.app.databinding.DialogPayDebtBinding
import com.warung.app.presentation.debt.DebtViewModel
import com.warung.app.presentation.debt.DebtViewModelFactory
import com.warung.app.utils.CurrencyFormatter

class PayDebtDialogFragment : DialogFragment() {

    private var _binding: DialogPayDebtBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DebtViewModel by activityViewModels {
        val app = requireActivity().application as AppWarungApplication
        DebtViewModelFactory(app.debtRepository)
    }

    private var customer: CustomerEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPayDebtBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupForm()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupForm() {
        customer?.let { c ->
            binding.tvPayCustomerName.text = "Pelanggan: ${c.name}"
            binding.tvPayRemainingDebt.text = CurrencyFormatter.formatRupiah(c.totalDebt)
            binding.etPayAmount.setText(c.totalDebt.toLong().toString())
        }
    }

    private fun setupListeners() {
        binding.btnPayInFull.setOnClickListener {
            customer?.let { c ->
                binding.etPayAmount.setText(c.totalDebt.toLong().toString())
            }
        }

        binding.btnCancelPay.setOnClickListener {
            dismiss()
        }

        binding.btnConfirmPay.setOnClickListener {
            validateAndPay()
        }
    }

    private fun validateAndPay() {
        val c = customer ?: return
        val payAmount = binding.etPayAmount.text?.toString()?.toDoubleOrNull() ?: 0.0
        val notes = binding.etPayNotes.text?.toString()?.trim()

        if (payAmount <= 0.0) {
            binding.tilPayAmount.error = "Nominal pembayaran harus lebih dari 0!"
            return
        }

        if (payAmount > c.totalDebt) {
            binding.tilPayAmount.error = "Nominal melebihi sisa hutang (${CurrencyFormatter.formatRupiah(c.totalDebt)})!"
            return
        }

        viewModel.payDebt(c.id, c.name, payAmount, notes)
        dismiss()
    }

    fun setCustomerData(customer: CustomerEntity) {
        this.customer = customer
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PayDebtDialog"

        fun newInstance(customer: CustomerEntity): PayDebtDialogFragment {
            return PayDebtDialogFragment().apply {
                setCustomerData(customer)
            }
        }
    }
}
