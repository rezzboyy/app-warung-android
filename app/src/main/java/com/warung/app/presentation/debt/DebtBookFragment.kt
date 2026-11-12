package com.warung.app.presentation.debt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.warung.app.AppWarungApplication
import com.warung.app.R
import com.warung.app.data.local.entity.CustomerEntity
import com.warung.app.databinding.FragmentDebtBookBinding
import com.warung.app.presentation.debt.adapter.CustomerDebtAdapter
import com.warung.app.presentation.debt.dialog.AddCustomerDebtDialogFragment
import com.warung.app.presentation.debt.dialog.PayDebtDialogFragment
import com.warung.app.utils.CurrencyFormatter
import com.warung.app.utils.Resource
import kotlinx.coroutines.launch
import java.net.URLEncoder

class DebtBookFragment : Fragment() {

    private var _binding: FragmentDebtBookBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DebtViewModel by activityViewModels {
        val app = requireActivity().application as AppWarungApplication
        DebtViewModelFactory(app.debtRepository)
    }

    private lateinit var customerAdapter: CustomerDebtAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebtBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        customerAdapter = CustomerDebtAdapter(
            onPayClick = { customer ->
                showPayDebtDialog(customer)
            },
            onAddDebtClick = { customer ->
                showAddDebtDialog(customer)
            },
            onWhatsAppClick = { customer ->
                sendWhatsAppReminder(customer)
            }
        )

        binding.rvDebtCustomers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = customerAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearchCustomer.doAfterTextChanged { text ->
            viewModel.search(text?.toString() ?: "")
        }
    }

    private fun setupFilters() {
        binding.cgDebtFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chip_filter_unpaid) -> viewModel.setFilter(DebtFilterMode.UNPAID_ONLY)
                checkedIds.contains(R.id.chip_filter_paid) -> viewModel.setFilter(DebtFilterMode.PAID_ONLY)
                else -> viewModel.setFilter(DebtFilterMode.ALL)
            }
        }
    }

    private fun setupListeners() {
        binding.fabAddCustomer.setOnClickListener {
            showAddDebtDialog(null)
        }
    }

    private fun showAddDebtDialog(customer: CustomerEntity?) {
        val dialog = AddCustomerDebtDialogFragment.newInstance(customer)
        dialog.show(childFragmentManager, AddCustomerDebtDialogFragment.TAG)
    }

    private fun showPayDebtDialog(customer: CustomerEntity) {
        val dialog = PayDebtDialogFragment.newInstance(customer)
        dialog.show(childFragmentManager, PayDebtDialogFragment.TAG)
    }

    private fun sendWhatsAppReminder(customer: CustomerEntity) {
        val rawPhone = customer.phoneNumber?.replace("[^0-9]".toRegex(), "") ?: ""
        if (rawPhone.isEmpty()) {
            Toast.makeText(requireContext(), "Nomor HP pelanggan belum terdaftar!", Toast.LENGTH_SHORT).show()
            return
        }

        // Format to international code for Indonesia: 08xx -> 628xx
        val phoneWithCountryCode = if (rawPhone.startsWith("0")) {
            "62" + rawPhone.substring(1)
        } else if (!rawPhone.startsWith("62")) {
            "62$rawPhone"
        } else {
            rawPhone
        }

        val message = "Halo ${customer.name}, mengingatkan catatan kasbon di warung sebesar ${CurrencyFormatter.formatRupiah(customer.totalDebt)}. Terima kasih banyak atas kerjasamanya 🙏"

        try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phoneWithCountryCode&text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Aplikasi WhatsApp tidak ditemukan di perangkat ini.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.totalDebt.collect { total ->
                        binding.tvTotalDebtAmount.text = CurrencyFormatter.formatRupiah(total)
                    }
                }

                launch {
                    viewModel.customersState.collect { state ->
                        handleCustomersState(state)
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

    private fun handleCustomersState(state: Resource<List<CustomerEntity>>) {
        when (state) {
            is Resource.Loading -> {
                binding.pbDebtLoading.visibility = View.VISIBLE
                binding.layoutDebtEmpty.visibility = View.GONE
            }
            is Resource.Success -> {
                binding.pbDebtLoading.visibility = View.GONE
                customerAdapter.submitList(state.data)

                val activeDebtors = state.data.count { it.totalDebt > 0.0 }
                binding.tvDebtCustomersCount.text = "$activeDebtors Pelanggan memiliki catatan kasbon aktif"

                if (state.data.isEmpty()) {
                    binding.rvDebtCustomers.visibility = View.GONE
                    binding.layoutDebtEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvDebtCustomers.visibility = View.VISIBLE
                    binding.layoutDebtEmpty.visibility = View.GONE
                }
            }
            is Resource.Error -> {
                binding.pbDebtLoading.visibility = View.GONE
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = DebtBookFragment()
    }
}
