package com.example.ecostyle.view

import android.content.Context
import android.content.SharedPreferences
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

class SubscriptionFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var plan1Button: Button
    private lateinit var plan2Button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        sharedPreferences = requireContext().getSharedPreferences("subscription_cache", Context.MODE_PRIVATE)
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

        // Retrieve cached plan
        val cachedPlan = sharedPreferences.getString("selected_plan", null)

        // Set click listeners
        plan1Button.setOnClickListener { handleButtonClick("Basic Plan", cachedPlan) }
        plan2Button.setOnClickListener { handleButtonClick("Premium Plan", cachedPlan) }
    }

    private fun handleButtonClick(plan: String, cachedPlan: String?) {
        if (cachedPlan != null) {
            // If a plan is already selected, show a message
            Toast.makeText(
                requireContext(),
                "You are already subscribed to $cachedPlan. Cannot change plan.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // If no plan is selected, proceed with subscription
            handleSubscription(plan)
        }
    }

    private fun handleSubscription(plan: String) {
        // Cache the selected plan
        sharedPreferences.edit().putString("selected_plan", plan).apply()

        progressBar.visibility = View.VISIBLE

        // Simulate subscription process
        progressBar.postDelayed({
            progressBar.visibility = View.GONE

            Toast.makeText(
                requireContext(),
                "Subscription successful for $plan!",
                Toast.LENGTH_LONG
            ).show()

            logSubscriptionEvent(plan)
        }, 2000)
    }

    private fun logSubscriptionEvent(plan: String) {
        firebaseAnalytics.logEvent("subscription_event") {
            param("subscription_plan", plan)
            param("subscription_status", "success")
        }
    }
}
