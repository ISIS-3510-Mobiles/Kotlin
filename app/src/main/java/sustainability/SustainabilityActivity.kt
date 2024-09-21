package sustainability

import com.example.ecostyle.R
import android.os.Bundle
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class SustainabilityActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sustainability)

        val wasteProgressBar = findViewById<ProgressBar>(R.id.circularProgressBarWaste)
        wasteProgressBar.progress = 100

        val progressWaterSaved = findViewById<ProgressBar>(R.id.progress_water_saved)
        progressWaterSaved.progress = 75

        val progressWasteDiverted = findViewById<ProgressBar>(R.id.progress_waste_diverted)
        progressWasteDiverted.progress = 50

        val progressCO2Prevented = findViewById<ProgressBar>(R.id.progress_co2_prevented)
        progressCO2Prevented.progress = 40
    }
}