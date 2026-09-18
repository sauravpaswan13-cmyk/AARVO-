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
            setPadding(28, 42, 28, 34)
            setBackgroundColor(Color.WHITE)
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "AARVO logo"
        }
        brand.addView(logo, LinearLayout.LayoutParams(72, 72))

        brand.addView(TextView(this).apply {
            text = "AARVO"
            textSize = 36f
            setTextColor(Color.rgb(31, 27, 110))
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = .04f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 0, 0, 0)
        }, LinearLayout.LayoutParams(-2, 72))

        root.addView(brand)

        root.addView(TextView(this).apply {
            text = "Your One Stop Shopping Destination"
            textSize = 15f
            setTextColor(Color.rgb(28, 32, 70))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }, LinearLayout.LayoutParams(-1, 48))

        // Clean premium welcome area: no bags, icons, offers or extra feature blocks.
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 0).apply {
            weight = 1f
        })

        val guest = Button(this).apply {
            text = "Continue as Guest"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = GradientDrawable().apply {
                cornerRadius = 30f
                setColor(Color.rgb(96, 31, 235))
            }
            setPadding(18, 0, 18, 0)
            setOnClickListener { enterApp() }
        }
        root.addView(guest, LinearLayout.LayoutParams(-1, 64).apply {
            bottomMargin = 16
        })

        val login = Button(this).apply {
            text = "Login / Sign Up"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(76, 34, 196))
            isAllCaps = false
            background = GradientDrawable().apply {
                cornerRadius = 30f
                setColor(Color.WHITE)
                setStroke(3, Color.rgb(102, 43, 230))
            }
            setPadding(18, 0, 18, 0)
            setOnClickListener { openLogin() }
        }
        root.addView(login, LinearLayout.LayoutParams(-1, 64).apply {
            bottomMargin = 8
        })

        setContentView(root)
    }

    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)

    private fun enterApp() {
        prefs().edit()
            .putBoolean("onboarded", true)
            .putBoolean("guest_mode", true)
            .putBoolean("signed_in", false)
            .apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun openLogin() {
        prefs().edit()
            .putBoolean("onboarded", true)
            .putBoolean("guest_mode", false)
            .apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java))
        finish()
    }
}
