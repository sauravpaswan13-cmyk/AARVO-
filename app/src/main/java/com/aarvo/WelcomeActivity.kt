package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 24, 28, 22)
            setBackgroundColor(Color.WHITE)
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "AARVO logo"
        }
        root.addView(logo, LinearLayout.LayoutParams(88, 72).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = 2
        })

        root.addView(TextView(this).apply {
            text = "AARVO"
            textSize = 30f
            setTextColor(Color.rgb(38, 35, 126))
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = .04f
        }, LinearLayout.LayoutParams(-1, -2))

        root.addView(TextView(this).apply {
            text = "Your One Stop Shopping Destination"
            textSize = 14f
            setTextColor(Color.rgb(70, 68, 96))
            gravity = Gravity.CENTER
            setPadding(0, 5, 0, 4)
        }, LinearLayout.LayoutParams(-1, -2))

        val bags = TextView(this).apply {
            text = "🛍️  🛍️  🛍️"
            textSize = 54f
            gravity = Gravity.CENTER
            setPadding(0, 14, 0, 10)
        }
        root.addView(bags, LinearLayout.LayoutParams(-1, 145))

        val guest = Button(this).apply {
            text = "Continue as Guest"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = GradientDrawable().apply {
                cornerRadius = 20f
                setColor(Color.rgb(88, 35, 220))
            }
            setOnClickListener { enterApp() }
        }
        root.addView(guest, LinearLayout.LayoutParams(-1, 58).apply { bottomMargin = 11 })

        val login = Button(this).apply {
            text = "Login / Sign Up"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(76, 38, 192))
            isAllCaps = false
            background = GradientDrawable().apply {
                cornerRadius = 20f
                setColor(Color.WHITE)
                setStroke(3, Color.rgb(103, 57, 220))
            }
            setOnClickListener { openLogin() }
        }
        root.addView(login, LinearLayout.LayoutParams(-1, 58).apply { bottomMargin = 10 })

        root.addView(TextView(this).apply {
            text = "Explore freely • Login when you want to buy or use account features"
            textSize = 12f
            setTextColor(Color.rgb(92, 88, 108))
            gravity = Gravity.CENTER
            setPadding(4, 0, 4, 0)
        }, LinearLayout.LayoutParams(-1, -2))

        val features = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 22, 0, 0)
        }
        listOf("✓\nSecure\nShopping", "▣\nFast\nDelivery", "♧\n24/7\nSupport", "★\nBest\nPrices").forEach { item ->
            features.addView(TextView(this).apply {
                text = item
                textSize = 11f
                setTextColor(Color.rgb(48, 38, 125))
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(0, 70, 1f))
        }
        root.addView(features, LinearLayout.LayoutParams(-1, 82))

        root.addView(TextView(this).apply {
            text = "━━━━━━━━━━━━"
            textSize = 10f
            setTextColor(Color.rgb(190, 188, 205))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 24).apply { topMargin = 4 })

        setContentView(root)
    }

    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)

    private fun enterApp() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", true).putBoolean("signed_in", false).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun openLogin() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", false).apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java))
        finish()
    }
}
