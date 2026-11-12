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
import com.warung.app.databinding.DialogAddCustomerDebtBinding
import com.warung.app.presentation.debt.DebtViewModel
import com.warung.app.presentation.debt.DebtViewModelFactory

class AddCustomerDebtDialogFragment : DialogFragment() {

    private var _binding: DialogAddCustomerDebtBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DebtViewModel by activityViewModels {
        val app = requireActivity().application as AppWarungApplication
        DebtViewModelFactory(app.debtRepository)
    }

    private var existingCustomer: CustomerEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddCustomerDebtBinding.inflate(inflater, container, false)
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
        existingCustomer?.let { customer ->
            binding.tvDialogTitle.text = "Tambah Kasbon: ${customer.name}"
            binding.etCustomerName.setText(customer.name)
            binding.etCustomerName.isEnabled = false
            binding.tilCustomerPhone.visibility = View.GONE
            binding.tilCustomerAddress.visibility = View.GONE
            binding.etDebtAmount.setText("")
        }
    }

    private fun setupListeners() {
        binding.btnCancelAddCustomer.setOnClickListener {
            dismiss()
        }

        binding.btnSaveAddCustomer.setOnClickListener {
            validateAndSave()
        }
    }

    private fun validateAndSave() {
        val name = binding.etCustomerName.text?.toString()?.trim() ?: ""
        val phone = binding.etCustomerPhone.text?.toString()?.trim()
        val address = binding.etCustomerAddress.text?.toString()?.trim()
        val debtAmount = binding.etDebtAmount.text?.toString()?.toDoubleOrNull() ?: 0.0
        val notes = binding.etDebtNotes.text?.toString()?.trim()

        if (name.isEmpty()) {
            binding.tilCustomerName.error = "Nama pelanggan wajib diisi!"
            return
        }

        existingCustomer?.let { customer ->
            if (debtAmount <= 0.0) {
                binding.tilDebtAmount.error = "Nominal kasbon harus lebih dari 0!"
                return
            }
            viewModel.addDebt(customer.id, customer.name, debtAmount, notes)
            dismiss()
            return
        }

        viewModel.saveCustomerWithDebt(
            name = name,
            phone = phone,
            address = address,
            initialDebt = debtAmount,
            notes = notes
        )
        dismiss()
    }

    fun setCustomer(customer: CustomerEntity) {
        this.existingCustomer = customer
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddCustomerDebtDialog"

        fun newInstance(customer: CustomerEntity? = null): AddCustomerDebtDialogFragment {
            return AddCustomerDebtDialogFragment().apply {
                if (customer != null) {
                    setCustomer(customer)
                }
            }
        }
    }
}
