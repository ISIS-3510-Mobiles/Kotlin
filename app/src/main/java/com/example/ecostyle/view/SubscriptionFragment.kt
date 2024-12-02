package com.example.ecostyle.view

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ecostyle.R
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SubscriptionFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var plan1Button: Button
    private lateinit var plan2Button: Button
    private lateinit var currentPlanTextView: TextView
    private lateinit var db: FirebaseFirestore
    private var currentPlan: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        sharedPreferences = requireContext().getSharedPreferences("subscription_cache", Context.MODE_PRIVATE)
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_subscription, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.subscription_progress_bar)
        plan1Button = view.findViewById(R.id.plan_1_subscribe_button)
        plan2Button = view.findViewById(R.id.plan_2_subscribe_button)
        currentPlanTextView = view.findViewById(R.id.current_plan_text_view)

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (!isOnline()) {
            // Offline: Use cached plan
            currentPlan = sharedPreferences.getString("selected_plan", null)
            updateUI()
            if (currentPlan != null) {
                Toast.makeText(
                    requireContext(),
                    "You are offline. Your current plan is: $currentPlan",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "You are offline. No subscription plan is cached.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            // Online: Check Firebase
            if (userId != null) {
                fetchPlanFromFirebase(userId)
            }
        }

        // Set button click listeners
        plan1Button.setOnClickListener { handleButtonClick("Basic Plan", userId) }
        plan2Button.setOnClickListener { handleButtonClick("Premium Plan", userId) }
    }

    private fun handleButtonClick(plan: String, userId: String?) {
        if (!isOnline()) {
            // Offline: Show specific message based on button clicked
            if (currentPlan == null) {
                Toast.makeText(requireContext(), "You are offline. No cached plan available to manage.", Toast.LENGTH_SHORT).show()
            } else if (plan == currentPlan) {
                Toast.makeText(requireContext(), "Wait until you have a connection to cancel your plan.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Wait until you have a connection to upgrade your plan.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        if (plan == currentPlan) {
            // Cancel current plan
            cancelPlan(userId)
        } else {
            // Subscribe to a new plan
            subscribeToPlan(plan, userId)
        }
    }

    private fun cancelPlan(userId: String) {
        progressBar.visibility = View.VISIBLE
        db.collection("users").document(userId).update("subscription_plan", null)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Plan canceled successfully!", Toast.LENGTH_LONG).show()
                currentPlan = null
                cachePlan(null)
                updateUI()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to cancel plan. Try again later.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun subscribeToPlan(plan: String, userId: String) {
        progressBar.visibility = View.VISIBLE
        db.collection("users").document(userId).set(mapOf("subscription_plan" to plan))
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Subscribed to $plan!", Toast.LENGTH_LONG).show()
                currentPlan = plan
                cachePlan(plan)
                updateUI()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to subscribe. Try again later.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI() {
        currentPlanTextView.text = if (currentPlan != null) {
            "Current Plan: $currentPlan"
        } else {
            "No plan selected"
        }

        plan1Button.text = when (currentPlan) {
            "Basic Plan" -> "Cancel Plan"
            else -> if (currentPlan != null) "Change to Basic Plan" else "Subscribe to Basic Plan"
        }

        plan2Button.text = when (currentPlan) {
            "Premium Plan" -> "Cancel Plan"
            else -> if (currentPlan != null) "Change to Premium Plan" else "Subscribe to Premium Plan"
        }
    }

    private fun cachePlan(plan: String?) {
        sharedPreferences.edit().putString("selected_plan", plan).apply()
    }

    private fun isOnline(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun fetchPlanFromFirebase(userId: String) {
        progressBar.visibility = View.VISIBLE
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                currentPlan = document.getString("subscription_plan")
                cachePlan(currentPlan) // Cache the fetched plan
                updateUI() // Update the UI with the fetched plan
                if (currentPlan != null) {
                    Toast.makeText(
                        requireContext(),
                        "You are online. Your current plan is: $currentPlan",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "You are online. No subscription plan is selected.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                currentPlan = sharedPreferences.getString("selected_plan", null)
                updateUI() // Update UI with the cached plan
                Toast.makeText(
                    requireContext(),
                    "Failed to fetch plan from server. Using cached data if available.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}