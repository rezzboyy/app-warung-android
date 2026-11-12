package com.warung.app.presentation.product

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.warung.app.AppWarungApplication
import com.warung.app.R
import com.warung.app.data.local.entity.CategoryEntity
import com.warung.app.data.local.entity.ProductEntity
import com.warung.app.databinding.DialogAddEditProductBinding
import com.warung.app.presentation.pos.dialog.BarcodeScannerDialogFragment

class AddEditProductDialogFragment : DialogFragment() {

    private var _binding: DialogAddEditProductBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductViewModel by activityViewModels {
        ProductViewModelFactory((requireActivity().application as AppWarungApplication).productRepository)
    }

    private var editingProduct: ProductEntity? = null
    private var categoryList: List<CategoryEntity> = emptyList()
    private var selectedCategoryId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUnitDropdown()
        setupCategoriesDropdown()
        populateFormIfEditing()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupUnitDropdown() {
        val units = listOf("Pcs", "Kg", "Bungkus", "Botol", "Renceng", "Dus", "Liter", "Sachet")
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        binding.actUnit.setAdapter(unitAdapter)
    }

    private fun setupCategoriesDropdown() {
        categoryList = viewModel.categories.value
        val categoryNames = categoryList.map { it.name }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
        binding.actProductCategory.setAdapter(categoryAdapter)

        binding.actProductCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = categoryList[position].id
        }
    }

    private fun populateFormIfEditing() {
        editingProduct?.let { product ->
            binding.tvDialogTitle.text = "Ubah Produk"
            binding.btnSave.text = "Perbarui Produk"

            binding.etProductName.setText(product.name)
            binding.etProductBarcode.setText(product.barcode ?: "")
            binding.etBuyPrice.setText(if (product.buyPrice > 0) product.buyPrice.toLong().toString() else "")
            binding.etSellPrice.setText(product.sellPrice.toLong().toString())
            binding.etStock.setText(product.stock.toString())
            binding.actUnit.setText(product.unit, false)

            selectedCategoryId = product.categoryId
            val category = categoryList.find { it.id == product.categoryId }
            if (category != null) {
                binding.actProductCategory.setText(category.name, false)
            }
        }
    }

    private fun setupListeners() {
        binding.tilProductBarcode.setEndIconOnClickListener {
            val scanner = BarcodeScannerDialogFragment.newInstance { scannedBarcode ->
                binding.etProductBarcode.setText(scannedBarcode)
            }
            scanner.show(childFragmentManager, BarcodeScannerDialogFragment.TAG)
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            validateAndSave()
        }
    }

    private fun validateAndSave() {
        val name = binding.etProductName.text?.toString()?.trim() ?: ""
        val barcode = binding.etProductBarcode.text?.toString()?.trim()
        val buyPriceText = binding.etBuyPrice.text?.toString()?.trim() ?: "0"
        val sellPriceText = binding.etSellPrice.text?.toString()?.trim() ?: ""
        val stockText = binding.etStock.text?.toString()?.trim() ?: ""
        val unit = binding.actUnit.text?.toString()?.trim() ?: "Pcs"

        var isValid = true

        if (name.isEmpty()) {
            binding.tilProductName.error = "Nama produk wajib diisi"
            isValid = false
        } else {
            binding.tilProductName.error = null
        }

        val sellPrice = sellPriceText.toDoubleOrNull() ?: 0.0
        if (sellPrice <= 0.0) {
            binding.tilSellPrice.error = "Harga jual harus lebih dari 0"
            isValid = false
        } else {
            binding.tilSellPrice.error = null
        }

        val stock = stockText.toIntOrNull() ?: 0
        if (stock < 0) {
            binding.tilStock.error = "Stok tidak boleh minus"
            isValid = false
        } else {
            binding.tilStock.error = null
        }

        if (!isValid) return

        val buyPrice = buyPriceText.toDoubleOrNull() ?: 0.0

        viewModel.saveProduct(
            id = editingProduct?.id ?: 0L,
            name = name,
            barcode = barcode,
            categoryId = selectedCategoryId,
            buyPrice = buyPrice,
            sellPrice = sellPrice,
            stock = stock,
            unit = unit
        )

        dismiss()
    }

    fun setProductToEdit(product: ProductEntity) {
        editingProduct = product
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddEditProductDialog"

        fun newInstance(product: ProductEntity? = null): AddEditProductDialogFragment {
            return AddEditProductDialogFragment().apply {
                if (product != null) {
                    setProductToEdit(product)
                }
            }
        }
    }
}
