package com.warung.app.presentation.updater

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.warung.app.data.updater.GitHubUpdateManager
import com.warung.app.data.updater.UpdateInfo
import com.warung.app.databinding.DialogUpdateAppBinding
import kotlinx.coroutines.launch

class AppUpdateDialogFragment : DialogFragment() {

    private var _binding: DialogUpdateAppBinding? = null
    private val binding get() = _binding!!

    private var isDownloading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUpdateAppBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tagName = arguments?.getString(ARG_TAG_NAME) ?: "v1.0.0"
        val releaseTitle = arguments?.getString(ARG_TITLE) ?: "Pembaruan Tersedia"
        val changelog = arguments?.getString(ARG_CHANGELOG) ?: "Peningkatan performa dan stabilitas aplikasi."
        val downloadUrl = arguments?.getString(ARG_DOWNLOAD_URL) ?: ""
        val apkSize = arguments?.getLong(ARG_APK_SIZE) ?: 0L

        val sizeFormatted = if (apkSize > 0) {
            String.format("%.1f MB", apkSize / (1024.0 * 1024.0))
        } else {
            "Siap Diunduh"
        }

        binding.tvUpdateTitle.text = releaseTitle
        binding.tvUpdateVersionBadge.text = "$tagName • $sizeFormatted"
        binding.tvUpdateChangelog.text = changelog

        binding.btnUpdateCancel.setOnClickListener {
            if (!isDownloading) {
                dismiss()
            }
        }

        binding.btnUpdateConfirm.setOnClickListener {
            if (downloadUrl.isEmpty()) {
                Toast.makeText(requireContext(), "Tautan berkas rilis tidak valid.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startDownload(downloadUrl)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog?.setCancelable(!isDownloading)
        dialog?.setCanceledOnTouchOutside(!isDownloading)
    }

    private fun startDownload(downloadUrl: String) {
        isDownloading = true
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)

        binding.tvUpdateTitle.text = "Mengunduh Pembaruan..."
        binding.btnUpdateCancel.visibility = View.GONE
        binding.btnUpdateConfirm.isEnabled = false
        binding.btnUpdateConfirm.text = "Mengunduh..."
        binding.layoutDownloadProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            GitHubUpdateManager.downloadAndInstallApk(
                context = requireContext().applicationContext,
                downloadUrl = downloadUrl,
                onProgress = { progress ->
                    val percent = (progress * 100).toInt()
                    binding.progressUpdateDownload.progress = percent
                    binding.tvDownloadProgressText.text = "$percent%"
                },
                onError = { errorMsg ->
                    isDownloading = false
                    dialog?.setCancelable(true)
                    binding.tvUpdateTitle.text = "Gagal Mengunduh"
                    binding.btnUpdateCancel.visibility = View.VISIBLE
                    binding.btnUpdateConfirm.isEnabled = true
                    binding.btnUpdateConfirm.text = "Coba Lagi"
                    binding.layoutDownloadProgress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Gagal: $errorMsg", Toast.LENGTH_LONG).show()
                }
            )
            // Selesai / dialihkan ke Package Installer
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AppUpdateDialogFragment"
        private const val ARG_TAG_NAME = "arg_tag_name"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_CHANGELOG = "arg_changelog"
        private const val ARG_DOWNLOAD_URL = "arg_download_url"
        private const val ARG_APK_SIZE = "arg_apk_size"

        fun newInstance(updateInfo: UpdateInfo): AppUpdateDialogFragment {
            return AppUpdateDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TAG_NAME, updateInfo.tagName)
                    putString(ARG_TITLE, updateInfo.releaseTitle)
                    putString(ARG_CHANGELOG, updateInfo.changelog)
                    putString(ARG_DOWNLOAD_URL, updateInfo.apkDownloadUrl)
                    putLong(ARG_APK_SIZE, updateInfo.apkSize)
                }
            }
        }
    }
}
