package com.warung.app.presentation.pos.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.warung.app.data.hardware.BluetoothPrinterManager
import com.warung.app.data.hardware.EscPosTicketBuilder
import com.warung.app.data.local.entity.TransactionEntity
import com.warung.app.data.local.entity.TransactionItemEntity
import com.warung.app.data.local.pref.StorePreference
import com.warung.app.databinding.DialogReceiptBinding
import com.warung.app.utils.CurrencyFormatter
import com.warung.app.utils.DateFormatter
import kotlinx.coroutines.launch

class ReceiptDialogFragment : DialogFragment() {

    private var _binding: DialogReceiptBinding? = null
    private val binding get() = _binding!!

    private var transaction: TransactionEntity? = null
    private var transactionItems: List<TransactionItemEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        populateReceiptData()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun populateReceiptData() {
        transaction?.let { tx ->
            binding.tvReceiptInvoice.text = "Invoice: ${tx.invoiceNumber}"
            binding.tvReceiptDate.text = DateFormatter.formatDateTime(tx.createdAt)
            binding.tvReceiptTotal.text = CurrencyFormatter.formatRupiah(tx.grandTotal)
            binding.tvReceiptPaymentMethod.text = "Bayar (${tx.paymentMethod})"
            binding.tvReceiptPaid.text = CurrencyFormatter.formatRupiah(tx.paidAmount)
            binding.tvReceiptChange.text = CurrencyFormatter.formatRupiah(tx.changeAmount)

            val itemsText = StringBuilder()
            transactionItems.forEach { item ->
                itemsText.append("${item.productName}  x${item.quantity}\n")
                itemsText.append("  @ ${CurrencyFormatter.formatRupiah(item.sellPrice)}  ->  ${CurrencyFormatter.formatRupiah(item.subtotal)}\n")
            }
            binding.tvReceiptItemsDetail.text = itemsText.toString().trimEnd()
        }
    }

    private fun setupListeners() {
        binding.btnReceiptPrint.setOnClickListener {
            printReceipt()
        }

        binding.btnReceiptNewTransaction.setOnClickListener {
            dismiss()
        }

        binding.btnReceiptClose.setOnClickListener {
            dismiss()
        }
    }

    private fun printReceipt() {
        val tx = transaction ?: return
        val storePref = StorePreference.getInstance(requireContext())
        val printerMac = storePref.selectedPrinterMac

        if (printerMac.isNullOrBlank()) {
            showPrinterPickerDialog { selectedMac, selectedName ->
                storePref.selectedPrinterMac = selectedMac
                storePref.selectedPrinterName = selectedName
                executePrint(tx, selectedMac, storePref)
            }
            return
        }

        executePrint(tx, printerMac, storePref)
    }

    private fun executePrint(tx: TransactionEntity, mac: String, pref: StorePreference) {
        val ticketBytes = EscPosTicketBuilder.buildReceipt(
            storeName = pref.storeName,
            storeAddress = pref.storeAddress,
            storePhone = pref.storePhone,
            invoiceNumber = tx.invoiceNumber,
            dateEpoch = tx.createdAt,
            items = transactionItems,
            grandTotal = tx.grandTotal,
            paidAmount = tx.paidAmount,
            changeAmount = tx.changeAmount,
            paymentMethod = tx.paymentMethod,
            footer = pref.receiptFooter
        )

        lifecycleScope.launch {
            binding.btnReceiptPrint.isEnabled = false
            binding.btnReceiptPrint.text = "Sedang Mencetak..."
            val result = BluetoothPrinterManager.printBytes(mac, ticketBytes)
            binding.btnReceiptPrint.isEnabled = true
            binding.btnReceiptPrint.text = "Cetak Struk Thermal (Bluetooth)"

            result.onSuccess {
                Toast.makeText(requireContext(), "Struk berhasil dicetak!", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(requireContext(), "Gagal mencetak: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showPrinterPickerDialog(onSelected: (mac: String, name: String) -> Unit) {
        val printers = BluetoothPrinterManager.getPairedPrinters()
        if (printers.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Printer Tidak Ditemukan")
                .setMessage("Belum ada printer Bluetooth yang terpasang (paired). Silakan pasangkan printer thermal Anda terlebih dahulu di pengaturan Bluetooth HP.")
                .setPositiveButton("Mengerti", null)
                .show()
            return
        }

        val names = printers.map { it.name ?: it.address }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pilih Printer Thermal")
            .setItems(names) { _, which ->
                val device = printers[which]
                onSelected(device.address, device.name ?: device.address)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    fun setTransactionData(
        tx: TransactionEntity,
        items: List<TransactionItemEntity>
    ) {
        this.transaction = tx
        this.transactionItems = items
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ReceiptDialog"

        fun newInstance(
            tx: TransactionEntity,
            items: List<TransactionItemEntity>
        ): ReceiptDialogFragment {
            return ReceiptDialogFragment().apply {
                setTransactionData(tx, items)
            }
        }
    }
}
