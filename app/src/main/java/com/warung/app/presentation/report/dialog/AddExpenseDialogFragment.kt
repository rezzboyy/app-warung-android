package com.warung.app.presentation.report.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.warung.app.AppWarungApplication
import com.warung.app.databinding.DialogAddExpenseBinding
import com.warung.app.presentation.report.ReportViewModel
import com.warung.app.presentation.report.ReportViewModelFactory

class AddExpenseDialogFragment : DialogFragment() {

    private var _binding: DialogAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportViewModel by activityViewModels {
        val app = requireActivity().application as AppWarungApplication
        ReportViewModelFactory(app.reportRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryDropdown()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupCategoryDropdown() {
        val categories = listOf(
            "Operasional (Es, Plastik, Gas)",
            "Kulakan / Stok Barang",
            "Listrik & Air",
            "Sewa Tempat",
            "Gaji / Upah",
            "Lain-lain"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.actExpenseCategory.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.btnCancelExpense.setOnClickListener {
            dismiss()
        }

        binding.btnSaveExpense.setOnClickListener {
            validateAndSave()
        }
    }

    private fun validateAndSave() {
        val amount = binding.etExpenseAmount.text?.toString()?.toDoubleOrNull() ?: 0.0
        val category = binding.actExpenseCategory.text?.toString()?.trim() ?: "Operasional"
        val desc = binding.etExpenseDesc.text?.toString()?.trim() ?: ""

        if (amount <= 0.0) {
            binding.tilExpenseAmount.error = "Nominal pengeluaran harus lebih dari 0!"
            return
        }

        if (desc.isEmpty()) {
            binding.tilExpenseDesc.error = "Keterangan pengeluaran wajib diisi!"
            return
        }

        viewModel.recordExpense(amount, category, desc)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddExpenseDialog"

        fun newInstance() = AddExpenseDialogFragment()
    }
}
