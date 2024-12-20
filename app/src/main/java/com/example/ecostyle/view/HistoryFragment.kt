package com.example.ecostyle.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.R
import com.example.ecostyle.adapter.HistoryAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var switchProducts: Switch
    private lateinit var historyTitle: TextView
    private lateinit var switchLabel: TextView
    private val productList = mutableListOf<Map<String, Any>>()
    private var isSalesHistory = false
    private val db = FirebaseFirestore.getInstance()
    private val userId by lazy { retrieveUserId() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_products)
        emptyView = view.findViewById(R.id.empty_view)
        switchProducts = view.findViewById(R.id.switch_products)
        historyTitle = view.findViewById(R.id.history_title)
        switchLabel = view.findViewById(R.id.switch_label)

        historyAdapter = HistoryAdapter(productList, isSalesHistory, userId)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = historyAdapter

        switchProducts.setOnCheckedChangeListener { _, isChecked ->
            isSalesHistory = isChecked
            if (isChecked) {
                historyTitle.text = "Sales history"
                switchLabel.text = "Show Purchased Products"
                historyAdapter = HistoryAdapter(productList, isSalesHistory = true, userId = userId)
                recyclerView.adapter = historyAdapter
                loadHistory("ventas")
            } else {
                historyTitle.text = "Purchase history"
                switchLabel.text = "Show Sold Products"
                historyAdapter = HistoryAdapter(productList, isSalesHistory = false, userId = userId)
                recyclerView.adapter = historyAdapter
                loadHistory("compras")
            }
        }

        historyTitle.text = "Purchase history"
        loadHistory("compras")

        return view
    }

    private fun loadHistory(type: String) {
        if (userId.isEmpty()) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = db.collection("historial").document(userId).get().await()
                if (document.exists()) {
                    val data = document.get(type) as? List<Map<String, Any>> ?: emptyList()
                    withContext(Dispatchers.Main) {
                        productList.clear()
                        productList.addAll(data)
                        historyAdapter.notifyDataSetChanged()
                        toggleEmptyView(null) // No hay errores, usar lógica normal
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        productList.clear()
                        toggleEmptyView(null) // No hay productos pero sin errores
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    productList.clear()
                    toggleEmptyView("Histories could not be loaded. Please check your internet connection and try again.")
                }
            }
        }
    }

    private fun toggleEmptyView(errorMessage: String?) {
        if (productList.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE

            // Mostrar mensaje de error si existe, o texto dinámico según el historial
            emptyView.text = errorMessage ?: if (isSalesHistory) {
                "You have not put any products for sale."
            } else {
                "You haven't bought anything yet, what are you waiting for to make your first purchase?"
            }
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun retrieveUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }
}
