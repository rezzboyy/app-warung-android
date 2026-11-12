package com.warung.app.presentation.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.warung.app.AppWarungApplication
import com.warung.app.R
import com.warung.app.data.local.pref.StorePreference
import com.warung.app.databinding.FragmentReportsBinding
import com.warung.app.presentation.report.adapter.ExpenseAdapter
import com.warung.app.presentation.report.dialog.AddExpenseDialogFragment
import com.warung.app.utils.CurrencyFormatter
import com.warung.app.utils.DateFormatter
import com.warung.app.utils.DocxReportExporter
import com.warung.app.utils.DocxReportType
import com.warung.app.utils.FullReportData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportViewModel by activityViewModels {
        val app = requireActivity().application as AppWarungApplication
        ReportViewModelFactory(app.reportRepository)
    }

    private lateinit var expenseAdapter: ExpenseAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter()
        binding.rvReportExpenses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseAdapter
        }
    }

    private fun setupFilters() {
        binding.cgReportTimeRange.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chip_range_today) -> viewModel.setTimeRange(TimeRangeFilter.TODAY)
                checkedIds.contains(R.id.chip_range_7days) -> viewModel.setTimeRange(TimeRangeFilter.LAST_7_DAYS)
                checkedIds.contains(R.id.chip_range_this_month) -> viewModel.setTimeRange(TimeRangeFilter.THIS_MONTH)
                checkedIds.contains(R.id.chip_range_all_time) -> viewModel.setTimeRange(TimeRangeFilter.ALL_TIME)
                else -> viewModel.setTimeRange(TimeRangeFilter.TODAY)
            }
        }
    }

    private fun setupListeners() {
        binding.btnRecordExpense.setOnClickListener {
            showAddExpenseDialog()
        }

        binding.btnExportCsv.setOnClickListener {
            showExportChooserDialog()
        }
    }

    private fun showExportChooserDialog() {
        val options = arrayOf(
            "📑 Laporan Lengkap Semua Riwayat (DOCX / Word)",
            "📄 Laporan Transaksi Kasir (DOCX / Word)",
            "📄 Laporan Pengeluaran Operasional (DOCX / Word)",
            "📄 Laporan Buku Kasbon Pelanggan (DOCX / Word)",
            "📄 Laporan Katalog Produk & Stok (DOCX / Word)",
            "📊 Ekspor Format Spreadsheet (CSV)"
        )

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ekspor Laporan Warung")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportDocxReport(DocxReportType.FULL)
                    1 -> exportDocxReport(DocxReportType.TRANSACTIONS)
                    2 -> exportDocxReport(DocxReportType.EXPENSES)
                    3 -> exportDocxReport(DocxReportType.DEBTS)
                    4 -> exportDocxReport(DocxReportType.PRODUCTS)
                    5 -> showCsvExportChooserDialog()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun exportDocxReport(type: DocxReportType) {
        lifecycleScope.launch {
            try {
                val app = requireActivity().application as AppWarungApplication
                val storePref = StorePreference.getInstance(requireContext())

                val currentRange = viewModel.timeRange.value
                val (startDate, endDate) = ReportViewModel.calculateDateRange(currentRange)
                val timeRangeLabel = when (currentRange) {
                    TimeRangeFilter.TODAY -> "Hari Ini (${DateFormatter.formatDate(System.currentTimeMillis())})"
                    TimeRangeFilter.LAST_7_DAYS -> "7 Hari Terakhir"
                    TimeRangeFilter.THIS_MONTH -> "Bulan Ini"
                    TimeRangeFilter.ALL_TIME -> "Semua Riwayat"
                }

                val transactions = if (currentRange == TimeRangeFilter.ALL_TIME) {
                    app.transactionRepository.getAllTransactions().first()
                } else {
                    app.transactionRepository.getTransactionsBetweenDates(startDate, endDate).first()
                }
                val transactionItems = app.transactionRepository.getAllTransactionItems().first()

                val expenses = if (currentRange == TimeRangeFilter.ALL_TIME) {
                    app.reportRepository.getExpensesBetweenDates(0L, Long.MAX_VALUE).first()
                } else {
                    app.reportRepository.getExpensesBetweenDates(startDate, endDate).first()
                }

                val customers = app.debtRepository.getAllCustomers().first()
                val totalDebt = customers.sumOf { it.totalDebt }

                val products = app.productRepository.getAllProducts().first()
                val categories = app.productRepository.getAllCategories().first()
                val categoryMap = categories.associate { it.id to it.name }

                val totalRevenue = transactions.sumOf { it.grandTotal }
                val totalExpense = expenses.sumOf { it.amount }
                val netProfit = totalRevenue - totalExpense

                val reportData = FullReportData(
                    storeName = storePref.storeName,
                    storeAddress = storePref.storeAddress,
                    storePhone = storePref.storePhone,
                    dateGenerated = DateFormatter.formatDateTime(System.currentTimeMillis()),
                    timeRange = timeRangeLabel,
                    totalRevenue = totalRevenue,
                    totalExpense = totalExpense,
                    netProfit = netProfit,
                    totalDebt = totalDebt,
                    transactions = transactions,
                    transactionItems = transactionItems,
                    expenses = expenses,
                    customers = customers,
                    products = products,
                    categoryMap = categoryMap
                )

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = when (type) {
                    DocxReportType.FULL -> "Laporan_Lengkap_Warung_$timeStamp.docx"
                    DocxReportType.TRANSACTIONS -> "Laporan_Transaksi_Kasir_$timeStamp.docx"
                    DocxReportType.EXPENSES -> "Laporan_Pengeluaran_Warung_$timeStamp.docx"
                    DocxReportType.DEBTS -> "Laporan_Buku_Kasbon_$timeStamp.docx"
                    DocxReportType.PRODUCTS -> "Laporan_Katalog_Produk_$timeStamp.docx"
                }

                val docxBytes = DocxReportExporter.generateDocxReport(reportData, type)
                DocxReportExporter.shareDocxFile(requireContext(), fileName, docxBytes)
                Toast.makeText(requireContext(), "Berhasil mengekspor dokumen Word (.docx)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal mengekspor laporan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showCsvExportChooserDialog() {
        val options = arrayOf(
            "Laporan Pengeluaran Operasional (CSV)",
            "Laporan Transaksi Kasir (CSV)",
            "Laporan Buku Kasbon Pelanggan (CSV)",
            "Katalog Produk & Stok (CSV)"
        )

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pilih Data untuk Diekspor (CSV)")
            .setItems(options) { _, which ->
                lifecycleScope.launch {
                    val app = requireActivity().application as AppWarungApplication
                    when (which) {
                        0 -> {
                            val expenses = viewModel.expenses.value
                            val csv = com.warung.app.utils.CsvExporter.exportExpensesToCsv(expenses)
                            com.warung.app.utils.CsvExporter.shareCsvContent(requireContext(), "Laporan_Pengeluaran.csv", csv)
                        }
                        1 -> {
                            val transactions = app.transactionRepository.getAllTransactions().first()
                            val csv = com.warung.app.utils.CsvExporter.exportTransactionsToCsv(transactions)
                            com.warung.app.utils.CsvExporter.shareCsvContent(requireContext(), "Laporan_Transaksi_Kasir.csv", csv)
                        }
                        2 -> {
                            val customers = app.debtRepository.getAllCustomers().first()
                            val csv = com.warung.app.utils.CsvExporter.exportDebtsToCsv(customers)
                            com.warung.app.utils.CsvExporter.shareCsvContent(requireContext(), "Laporan_Buku_Kasbon.csv", csv)
                        }
                        3 -> {
                            val products = app.productRepository.getAllProducts().first()
                            val csv = com.warung.app.utils.CsvExporter.exportProductsToCsv(products)
                            com.warung.app.utils.CsvExporter.shareCsvContent(requireContext(), "Katalog_Produk_Stok.csv", csv)
                        }
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showAddExpenseDialog() {
        val dialog = AddExpenseDialogFragment.newInstance()
        dialog.show(childFragmentManager, AddExpenseDialogFragment.TAG)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.summary.collect { summary ->
                        binding.tvReportRevenue.text = CurrencyFormatter.formatRupiah(summary.totalRevenue)
                        binding.tvReportExpense.text = CurrencyFormatter.formatRupiah(summary.totalExpense)
                        binding.tvReportNetProfit.text = CurrencyFormatter.formatRupiah(summary.netProfit)
                        binding.tvReportTransactions.text = "${summary.totalTransactions} Transaksi"
                    }
                }

                launch {
                    viewModel.expenses.collect { list ->
                        expenseAdapter.submitList(list)

                        if (list.isEmpty()) {
                            binding.layoutReportEmpty.visibility = View.VISIBLE
                            binding.rvReportExpenses.visibility = View.GONE
                        } else {
                            binding.layoutReportEmpty.visibility = View.GONE
                            binding.rvReportExpenses.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.userMessage.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ReportFragment()
    }
}
