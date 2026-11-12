package com.warung.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.warung.app.data.updater.GitHubUpdateManager
import com.warung.app.data.updater.UpdateCheckResult
import com.warung.app.databinding.ActivityMainBinding
import com.warung.app.presentation.debt.DebtBookFragment
import com.warung.app.presentation.pos.PosFragment
import com.warung.app.presentation.product.ProductListFragment
import com.warung.app.presentation.report.ReportFragment
import com.warung.app.presentation.settings.SettingsDialogFragment
import com.warung.app.presentation.updater.AppUpdateDialogFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigation()

        if (savedInstanceState == null) {
            // Default fragment on startup: POS
            loadFragment(PosFragment.newInstance(), "Mesin Kasir (POS)")
            binding.bottomNavigation.selectedItemId = R.id.nav_pos
        }

        // Pengecekan otomatis rilis versi baru di latar belakang
        checkForAppUpdateQuietly()
    }

    private fun checkForAppUpdateQuietly() {
        lifecycleScope.launch {
            try {
                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
                when (val result = GitHubUpdateManager.checkForUpdate(currentVersion)) {
                    is UpdateCheckResult.UpdateAvailable -> {
                        if (!isFinishing && !isDestroyed) {
                            AppUpdateDialogFragment.newInstance(result.info)
                                .show(supportFragmentManager, AppUpdateDialogFragment.TAG)
                        }
                    }
                    else -> {}
                }
            } catch (_: Exception) {}
        }
    }

    private fun setupToolbar() {
        binding.toolbarMain.inflateMenu(R.menu.toolbar_menu)
        binding.toolbarMain.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    SettingsDialogFragment.newInstance().show(supportFragmentManager, SettingsDialogFragment.TAG)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_pos -> {
                    loadFragment(PosFragment.newInstance(), "Mesin Kasir (POS)")
                    true
                }
                R.id.nav_products -> {
                    loadFragment(ProductListFragment.newInstance(), "Katalog Produk & Stok")
                    true
                }
                R.id.nav_debts -> {
                    loadFragment(DebtBookFragment.newInstance(), "Buku Kasbon Pelanggan")
                    true
                }
                R.id.nav_reports -> {
                    loadFragment(ReportFragment.newInstance(), "Laporan & Arus Kas")
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment, title: String) {
        binding.toolbarMain.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
