package com.example.variantsdk
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.variant.android.core.Variant

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple programmatic UI to test the SDK
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val textView = TextView(this).apply {
            // Test the SDK getString logic
            val color = Variant.getString("btn_color", "Gray")
            text = "Assigned Variant: $color"
            textSize = 24f
        }

        val button = Button(this).apply {
            text = "Track Event"
            setOnClickListener {
                // Test the SDK tracking logic
                Variant.track("btn_color", "button_clicked")
            }
        }

        layout.addView(textView)
        layout.addView(button)
        setContentView(layout)
    }
}