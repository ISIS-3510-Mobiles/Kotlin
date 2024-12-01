package com.example.ecostyle.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.R
import com.example.ecostyle.adapter.HistoryAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var switchProducts: Switch
    private val productList = mutableListOf<Map<String, Any>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_products)
        switchProducts = view.findViewById(R.id.switch_products)

        // Configurar el RecyclerView y el Adaptador
        historyAdapter = HistoryAdapter(productList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = historyAdapter

        // Configurar el Switch para alternar entre ventas y compras
        switchProducts.setOnCheckedChangeListener { _, isChecked ->
            loadHistory(if (isChecked) "ventas" else "compras")
        }

        // Cargar datos iniciales (compras por defecto)
        loadHistory("compras")

        return view
    }

    private fun loadHistory(type: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        if (userId != null) {
            db.collection("historial").document(userId).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    productList.clear()
                    val data = document.get(type) as List<Map<String, Any>>?
                    if (data != null) {
                        productList.addAll(data)
                    }
                    historyAdapter.notifyDataSetChanged()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load history: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}
