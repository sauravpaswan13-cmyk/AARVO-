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
import android.widget.Space
import android.widget.TextView
import androidx.activity.ComponentActivity

class WelcomeActivity : ComponentActivity() {
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(28))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.WHITE,
                    Color.rgb(249, 246, 255),
                    Color.rgb(241, 237, 255)
                )
            )
        }

        root.addView(Space(this), LinearLayout.LayoutParams(1, 0, 0.22f))

        root.addView(TextView(this).apply {
            text = "AARVO"
            textSize = 44f
            setTextColor(Color.rgb(54, 24, 145))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = .055f
        }, LinearLayout.LayoutParams(-1, dp(62)))

        root.addView(TextView(this).apply {
            text = "Your One Stop Shopping Destination"
            textSize = 16f
            setTextColor(Color.rgb(78, 69, 108))
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.NORMAL)
        }, LinearLayout.LayoutParams(-1, dp(34)))

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val trolley = TextView(this@WelcomeActivity).apply {
                text = "🛒"
                textSize = 88f
                gravity = Gravity.CENTER
                includeFontPadding = true
            }
            addView(trolley, LinearLayout.LayoutParams(-1, dp(118)))
        }, LinearLayout.LayoutParams(-1, 0, 0.50f))

        val guest = Button(this).apply {
            text = "Guest"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            isAllCaps = false
            stateListAnimator = null
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.rgb(88, 24, 224),
                    Color.rgb(128, 37, 239),
                    Color.rgb(225, 53, 157)
                )
            ).apply { cornerRadius = dp(18).toFloat() }
            setOnClickListener { enterGuest() }
        }
        root.addView(guest, LinearLayout.LayoutParams(-1, dp(58)).apply {
            bottomMargin = dp(14)
        })

        val login = Button(this).apply {
            text = "Login / Sign Up"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(83, 30, 190))
            isAllCaps = false
            stateListAnimator = null
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(2), Color.rgb(105, 48, 220))
            }
            setOnClickListener { openLogin() }
        }
        root.addView(login, LinearLayout.LayoutParams(-1, dp(58)))

        root.addView(Space(this), LinearLayout.LayoutParams(1, 0, 0.18f))

        setContentView(root)
    }

    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)

    private fun enterGuest() {
        prefs().edit()
            .putBoolean("onboarded", true)
            .putBoolean("guest_mode", true)
            .putBoolean("signed_in", false)
            .putString("user_role", "BUYER")
            .remove("auth_token")
            .apply()
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private fun openLogin() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", false).apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java))
        finish()
    }
}
