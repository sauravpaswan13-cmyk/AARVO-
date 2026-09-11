package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 40, 28, 32)
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.WHITE, Color.rgb(248, 246, 255))
            )
        }

        val title = TextView(this).apply {
            text = "AARVO"
            textSize = 38f
            setTextColor(Color.rgb(46, 23, 140))
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val subtitle = TextView(this).apply {
            text = "Your One Stop Shopping Destination"
            textSize = 15f
            setTextColor(Color.rgb(93, 93, 112))
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 0)
        }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2))

        val bags = TextView(this).apply {
            text = "🛍️   🛍️   🛍️"
            textSize = 30f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 26)
        }
        root.addView(bags, LinearLayout.LayoutParams(-1, -2))

        val guest = Button(this).apply {
            text = "Continue as Guest"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(Color.rgb(46, 23, 140))
            }
            setOnClickListener { enterGuest() }
        }
        root.addView(guest, LinearLayout.LayoutParams(-1, 56).apply { bottomMargin = 12 })

        val login = Button(this).apply {
            text = "Login / Sign Up"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(46, 23, 140))
            background = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(Color.WHITE)
                setStroke(3, Color.rgb(46, 23, 140))
            }
            setOnClickListener { openLogin() }
        }
        root.addView(login, LinearLayout.LayoutParams(-1, 56).apply { bottomMargin = 10 })

        val explore = TextView(this).apply {
            text = "Explore as Guest"
            textSize = 15f
            setTextColor(Color.rgb(46, 23, 140))
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 10, 0, 18)
            setOnClickListener { enterGuest() }
        }
        root.addView(explore, LinearLayout.LayoutParams(-1, -2))

        val trust = TextView(this).apply {
            text = "Secure Shopping  •  Trusted Support  •  Fast Delivery"
            textSize = 12f
            setTextColor(Color.rgb(102, 96, 112))
            gravity = Gravity.CENTER
        }
        root.addView(trust, LinearLayout.LayoutParams(-1, -2))

        setContentView(root)
    }

    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)

    private fun enterGuest() {
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
