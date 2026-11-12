package com.warung.app.presentation.pos.dialog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.warung.app.AppWarungApplication
import com.warung.app.R
import com.warung.app.databinding.DialogScanBarcodeBinding
import com.warung.app.presentation.pos.PosViewModel
import com.warung.app.presentation.pos.PosViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScannerDialogFragment : DialogFragment() {

    private var _binding: DialogScanBarcodeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PosViewModel by activityViewModels {
        val app = requireActivity().application as AppWarungApplication
        PosViewModelFactory(app.productRepository, app.transactionRepository)
    }

    private var cameraExecutor: ExecutorService? = null
    private var camera: Camera? = null
    private var isTorchOn = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    @Volatile
    private var isBarcodeProcessed = false

    var onBarcodeScanned: ((String) -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            binding.layoutPermissionDenied.visibility = View.GONE
            startCamera()
        } else {
            binding.layoutPermissionDenied.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogScanBarcodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        checkCameraPermissionAndStart()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun checkCameraPermissionAndStart() {
        val context = requireContext()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            binding.layoutPermissionDenied.visibility = View.GONE
            startCamera()
        } else {
            binding.layoutPermissionDenied.visibility = View.VISIBLE
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val context = context ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Preview setup
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

                // Barcode options: Support standard retail 1D/2D barcodes
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_QR_CODE
                    )
                    .build()
                val barcodeScanner = BarcodeScanning.getClient(options)

                // Image Analysis
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val executor = cameraExecutor ?: return@addListener
                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    analyzeImage(barcodeScanner, imageProxy)
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                // Re-apply torch if already on
                if (isTorchOn) {
                    camera?.cameraControl?.enableTorch(true)
                }
            } catch (e: Exception) {
                // Ignore or handle camera start exception gracefully
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyzeImage(
        barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !isBarcodeProcessed) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (!isBarcodeProcessed && barcodes.isNotEmpty()) {
                        val rawBarcode = barcodes.firstOrNull()?.rawValue?.trim()
                        if (!rawBarcode.isNullOrEmpty()) {
                            isBarcodeProcessed = true
                            activity?.runOnUiThread {
                                triggerSuccessFeedback()
                                handleScannedBarcode(rawBarcode)
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleScannedBarcode(barcode: String) {
        val customCallback = onBarcodeScanned
        if (customCallback != null) {
            customCallback.invoke(barcode)
            dismiss()
        } else {
            // Default POS workflow: Query database and insert to cart
            processBarcodeLookup(barcode)
        }
    }

    private fun setupListeners() {
        binding.btnCloseDialog.setOnClickListener {
            dismiss()
        }

        binding.btnCancelBarcodeScan.setOnClickListener {
            dismiss()
        }

        binding.btnRequestPermission.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnToggleFlash.setOnClickListener {
            toggleFlashlight()
        }

        binding.btnSwitchCamera.setOnClickListener {
            switchCameraLens()
        }

        binding.btnConfirmBarcodeScan.setOnClickListener {
            val barcode = binding.etBarcodeScanInput.text?.toString()?.trim() ?: ""
            if (barcode.isEmpty()) {
                binding.tilBarcodeScanInput.error = "Ketik atau scan kode barcode!"
                return@setOnClickListener
            }
            triggerSuccessFeedback()
            handleScannedBarcode(barcode)
        }

        binding.etBarcodeScanInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val barcode = binding.etBarcodeScanInput.text?.toString()?.trim() ?: ""
                if (barcode.isNotEmpty()) {
                    triggerSuccessFeedback()
                    handleScannedBarcode(barcode)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }

    private fun toggleFlashlight() {
        val cam = camera ?: return
        isTorchOn = !isTorchOn
        cam.cameraControl.enableTorch(isTorchOn)
        binding.btnToggleFlash.setImageResource(
            if (isTorchOn) R.drawable.ic_flash_off_24 else R.drawable.ic_flash_on_24
        )
    }

    private fun switchCameraLens() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        isTorchOn = false
        binding.btnToggleFlash.setImageResource(R.drawable.ic_flash_on_24)
        startCamera()
    }

    private fun processBarcodeLookup(barcode: String) {
        lifecycleScope.launch {
            val app = requireActivity().application as AppWarungApplication
            val product = app.productRepository.getProductByBarcode(barcode).first()

            if (product != null) {
                viewModel.addToCart(product)
                Toast.makeText(
                    requireContext(),
                    "'${product.name}' ditambahkan ke keranjang!",
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            } else {
                isBarcodeProcessed = false // allow scanning again or retry
                binding.tilBarcodeScanInput.error = "Produk dengan barcode '$barcode' tidak ditemukan!"
                binding.etBarcodeScanInput.setText(barcode)
            }
        }
    }

    private fun triggerSuccessFeedback() {
        try {
            val ctx = context ?: return

            // Audio beep
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120)

            // Haptic vibration
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {
            // Ignore if vibration or tone generator not supported on host device
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor?.shutdown()
        cameraExecutor = null
        _binding = null
    }

    companion object {
        const val TAG = "BarcodeScannerDialog"

        fun newInstance(onBarcodeScanned: ((String) -> Unit)? = null): BarcodeScannerDialogFragment {
            val dialog = BarcodeScannerDialogFragment()
            dialog.onBarcodeScanned = onBarcodeScanned
            return dialog
        }
    }
}
