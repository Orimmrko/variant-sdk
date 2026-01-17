package com.example.variantsdk

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.variant.android.core.Variant

class MainActivity : AppCompatActivity(), Variant.VariantListener {
    private lateinit var textView: TextView
    private lateinit var trackButton: Button
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Define Safety Fallbacks (If server is down)
        val safetyDefaults = mapOf(
            "btn_color" to "Gray",
            "cta_text" to "Click Me",
            "font_size" to "20"
        )

        // 2. Initialize SDK
        Variant.init(this, "your_api_key", safetyDefaults)
        Variant.setListener(this)

        // 3. Create Programmatic UI
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        textView = TextView(this).apply {
            text = "Loading Variants..."
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }
        layout.addView(textView)

        trackButton = Button(this).apply {
            setOnClickListener {
                // Track for both major experiments
                Variant.track("btn_color", "button_clicked")
                Variant.track("cta_text", "button_clicked")
                Variant.track("font_size", "button_clicked")
            }
        }
        layout.addView(trackButton)

        resetButton = Button(this).apply {
            text = "Reset User (New Bucket)"
            setOnClickListener {
                Variant.resetUser(this@MainActivity)
                textView.text = "Resetting User Identity..."
                refreshUI()
            }
        }
        layout.addView(resetButton)

        setContentView(layout)

        // Initial UI state

    }

    override fun onConfigUpdated() {
        Log.d("MainActivity", "Config updated from server. Refreshing UI.")
        refreshUI()
    }

    private fun refreshUI() {
        // 1. Fetch values from SDK
        val colorName = Variant.getString("btn_color", "Gray")
        val buttonText = Variant.getString("cta_text", "Track Click")
        val sizeString = Variant.getString("font_size", "24")

        val fontSize = sizeString.toFloatOrNull() ?: 24f

        // 2. Update UI Components
        textView.text = "Experiment Values:\nColor: $colorName | Size: $sizeString"
        textView.textSize = fontSize
        trackButton.text = buttonText

        val colorInt = when (colorName.lowercase()) {
            "red" -> android.graphics.Color.RED
            "blue" -> android.graphics.Color.BLUE
            "green" -> android.graphics.Color.GREEN
            else -> android.graphics.Color.GRAY
        }
        textView.setTextColor(colorInt)
    }
}