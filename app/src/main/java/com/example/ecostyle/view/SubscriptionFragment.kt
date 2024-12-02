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
    private lateinit var db: FirebaseFirestore

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

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (!isOnline()) {
            // Offline: Use cached plan
            val cachedPlan = sharedPreferences.getString("selected_plan", null)
            if (cachedPlan != null) {
                Toast.makeText(
                    requireContext(),
                    "You are offline. Your current plan is: $cachedPlan",
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

    private fun fetchPlanFromFirebase(userId: String) {
        progressBar.visibility = View.VISIBLE
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                val plan = document.getString("subscription_plan")
                if (plan != null) {
                    // Plan exists in Firebase
                    Toast.makeText(
                        requireContext(),
                        "You are online. Your current plan is: $plan",
                        Toast.LENGTH_LONG
                    ).show()
                    cachePlan(plan) // Update cache
                } else {
                    // No plan in Firebase
                    Toast.makeText(
                        requireContext(),
                        "You are online. No subscription plan is selected.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to fetch plan from server.", Toast.LENGTH_SHORT).show()
                val cachedPlan = sharedPreferences.getString("selected_plan", null)
                if (cachedPlan != null) {
                    Toast.makeText(
                        requireContext(),
                        "Using cached plan: $cachedPlan",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No cached plan available.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun handleButtonClick(plan: String, userId: String?) {
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isOnline()) {
            // Online: Check Firebase first
            progressBar.visibility = View.VISIBLE
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    progressBar.visibility = View.GONE
                    val currentPlan = document.getString("subscription_plan")
                    if (currentPlan != null) {
                        // User already has a plan
                        Toast.makeText(
                            requireContext(),
                            "You are already subscribed to $currentPlan. Cannot change plan.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // No plan exists, allow subscription
                        handleSubscription(plan, userId)
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to check subscription status.", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Offline: Use cached data
            val cachedPlan = sharedPreferences.getString("selected_plan", null)
            if (cachedPlan != null) {
                Toast.makeText(
                    requireContext(),
                    "You are offline and already subscribed to $cachedPlan.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(requireContext(), "You are offline. Unable to subscribe.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSubscription(plan: String, userId: String) {
        progressBar.visibility = View.VISIBLE
        val userDoc = db.collection("users").document(userId)
        userDoc.set(mapOf("subscription_plan" to plan))
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Subscription successful for $plan!",
                    Toast.LENGTH_LONG
                ).show()

                logSubscriptionEvent(plan)
                cachePlan(plan) // Cache the plan locally
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to subscribe. Try again later.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logSubscriptionEvent(plan: String) {
        firebaseAnalytics.logEvent("subscription_event") {
            param("subscription_plan", plan)
            param("subscription_status", "success")
        }
    }

    private fun cachePlan(plan: String) {
        sharedPreferences.edit().putString("selected_plan", plan).apply()
    }

    private fun isOnline(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
