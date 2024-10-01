package com.example.ecostyle.view

import com.example.ecostyle.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment

class SustainabilityFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el diseño para este fragmento
        val view = inflater.inflate(R.layout.activity_sustainability, container, false)

        val wasteProgressBar = view.findViewById<ProgressBar>(R.id.circularProgressBarWaste)
        wasteProgressBar.progress = 100

        val progressWaterSaved = view.findViewById<ProgressBar>(R.id.progress_water_saved)
        progressWaterSaved.progress = 75

        val progressWasteDiverted = view.findViewById<ProgressBar>(R.id.progress_waste_diverted)
        progressWasteDiverted.progress = 50

        val progressCO2Prevented = view.findViewById<ProgressBar>(R.id.progress_co2_prevented)
        progressCO2Prevented.progress = 40

        return view
    }
}
