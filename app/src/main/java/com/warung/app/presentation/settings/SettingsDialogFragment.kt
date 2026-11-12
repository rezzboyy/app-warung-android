package com.warung.app.presentation.settings

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
import com.warung.app.data.local.pref.StorePreference
import com.warung.app.data.updater.GitHubUpdateManager
import com.warung.app.data.updater.UpdateCheckResult
import com.warung.app.databinding.DialogSettingsBinding
import com.warung.app.presentation.updater.AppUpdateDialogFragment
import com.warung.app.utils.Constants
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SettingsDialogFragment : DialogFragment() {

    private var _binding: DialogSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var storePref: StorePreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        storePref = StorePreference.getInstance(requireContext())

        populateSettings()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun populateSettings() {
        binding.etSettingsName.setText(storePref.storeName)
        binding.etSettingsAddress.setText(storePref.storeAddress)
        binding.etSettingsPhone.setText(storePref.storePhone)
        binding.etSettingsFooter.setText(storePref.receiptFooter)

        updatePrinterInfo()
    }

    private fun updatePrinterInfo() {
        val printerName = storePref.selectedPrinterName
        val printerMac = storePref.selectedPrinterMac

        if (!printerMac.isNullOrBlank()) {
            binding.tvSettingsPrinterInfo.text = "Printer: ${printerName ?: "Perangkat Bluetooth"} ($printerMac)"
        } else {
            binding.tvSettingsPrinterInfo.text = "Printer: Belum Terpilih"
        }
    }

    private fun setupListeners() {
        binding.btnSettingsSelectPrinter.setOnClickListener {
            showPrinterPickerDialog()
        }

        binding.btnSettingsTestPrint.setOnClickListener {
            executeTestPrint()
        }

        binding.btnSettingsBackupDb.setOnClickListener {
            backupDatabase()
        }

        binding.btnSettingsCheckUpdate.setOnClickListener {
            checkAppUpdateManually()
        }

        binding.btnCancelSettings.setOnClickListener {
            dismiss()
        }

        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun showPrinterPickerDialog() {
        val printers = BluetoothPrinterManager.getPairedPrinters()
        if (printers.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tidak Ada Perangkat")
                .setMessage("Belum ada printer Bluetooth yang dipasangkan (paired). Pasangkan printer thermal Anda terlebih dahulu di menu Bluetooth HP.")
                .setPositiveButton("Mengerti", null)
                .show()
            return
        }

        val names = printers.map { "${it.name ?: "Printer"} (${it.address})" }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pilih Printer Thermal")
            .setItems(names) { _, which ->
                val device = printers[which]
                storePref.selectedPrinterMac = device.address
                storePref.selectedPrinterName = device.name ?: device.address
                updatePrinterInfo()
                Toast.makeText(requireContext(), "Printer terpilih: ${device.name ?: device.address}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun executeTestPrint() {
        val mac = storePref.selectedPrinterMac
        if (mac.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Pilih printer Bluetooth terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        val testBytes = EscPosTicketBuilder.buildTestTicket(
            storeName = storePref.storeName,
            storeAddress = storePref.storeAddress
        )

        lifecycleScope.launch {
            binding.btnSettingsTestPrint.isEnabled = false
            val result = BluetoothPrinterManager.printBytes(mac, testBytes)
            binding.btnSettingsTestPrint.isEnabled = true

            result.onSuccess {
                Toast.makeText(requireContext(), "Test print berhasil dikirim!", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(requireContext(), "Test print gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun backupDatabase() {
        try {
            val dbFile = requireContext().getDatabasePath(Constants.DATABASE_NAME)
            if (!dbFile.exists()) {
                Toast.makeText(requireContext(), "Basis data belum memiliki transaksi.", Toast.LENGTH_SHORT).show()
                return
            }

            val backupDir = File(requireContext().cacheDir, "backups").apply { mkdirs() }
            val backupFile = File(backupDir, "backup_${Constants.DATABASE_NAME}")

            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            Toast.makeText(requireContext(), "Database berhasil dicadangkan (${backupFile.length() / 1024} KB)!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal mencadangkan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAppUpdateManually() {
        binding.btnSettingsCheckUpdate.isEnabled = false
        binding.btnSettingsCheckUpdate.text = "Memeriksa Pembaruan..."

        lifecycleScope.launch {
            try {
                val currentVersion = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName ?: "1.0.0"
                when (val result = GitHubUpdateManager.checkForUpdate(currentVersion)) {
                    is UpdateCheckResult.UpdateAvailable -> {
                        binding.btnSettingsCheckUpdate.isEnabled = true
                        binding.btnSettingsCheckUpdate.text = "Periksa Pembaruan Sistem (OTA)"
                        AppUpdateDialogFragment.newInstance(result.info)
                            .show(parentFragmentManager, AppUpdateDialogFragment.TAG)
                    }
                    is UpdateCheckResult.UpToDate -> {
                        binding.btnSettingsCheckUpdate.isEnabled = true
                        binding.btnSettingsCheckUpdate.text = "Periksa Pembaruan Sistem (OTA)"
                        Toast.makeText(requireContext(), "Aplikasi sudah versi terbaru ($currentVersion)", Toast.LENGTH_SHORT).show()
                    }
                    is UpdateCheckResult.Error -> {
                        binding.btnSettingsCheckUpdate.isEnabled = true
                        binding.btnSettingsCheckUpdate.text = "Periksa Pembaruan Sistem (OTA)"
                        Toast.makeText(requireContext(), "Gagal memeriksa: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                binding.btnSettingsCheckUpdate.isEnabled = true
                binding.btnSettingsCheckUpdate.text = "Periksa Pembaruan Sistem (OTA)"
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveSettings() {
        val name = binding.etSettingsName.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) {
            binding.tilSettingsName.error = "Nama warung wajib diisi!"
            return
        }

        storePref.storeName = name
        storePref.storeAddress = binding.etSettingsAddress.text?.toString()?.trim() ?: ""
        storePref.storePhone = binding.etSettingsPhone.text?.toString()?.trim() ?: ""
        storePref.receiptFooter = binding.etSettingsFooter.text?.toString()?.trim() ?: ""

        Toast.makeText(requireContext(), "Pengaturan berhasil disimpan!", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SettingsDialog"

        fun newInstance() = SettingsDialogFragment()
    }
}
