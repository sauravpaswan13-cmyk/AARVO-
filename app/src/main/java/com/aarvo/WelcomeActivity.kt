package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the real status/navigation bars visible like the supplied reference.
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFFFDF9FF.toInt(), 0xFFF4F0FF.toInt(), 0xFFF1F7FF.toInt())
            )
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(28), dp(28), dp(28))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(34).toFloat()
            }
            elevation = dp(12).toFloat()
        }
        val cardLp = FrameLayout.LayoutParams(-1, -2).apply {
            leftMargin = dp(28)
            rightMargin = dp(28)
            gravity = Gravity.CENTER
        }
        root.addView(card, cardLp)

        val heroCircle = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(0xFFEDE5FA.toInt())
                shape = GradientDrawable.OVAL
            }
            elevation = dp(8).toFloat()
            contentDescription = "AARVO logo"
        }
        val heroLogo = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_entry_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO logo"
        }
        heroCircle.addView(heroLogo, FrameLayout.LayoutParams(dp(112), dp(112)).apply {
            gravity = Gravity.CENTER
        })
        card.addView(heroCircle, LinearLayout.LayoutParams(dp(150), dp(150)).apply {
            topMargin = dp(6)
        })

        val title = TextView(this).apply {
            text = "AARVO"
            textSize = 34f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        card.addView(title, LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(4)
        })

        val tagline = TextView(this).apply {
            text = "Shop Smart • Live Better"
            textSize = 20f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        card.addView(tagline, LinearLayout.LayoutParams(-1, dp(34)))

        val description = TextView(this).apply {
            text = "Discover products. Shop freely. Enjoy\nAARVO."
            textSize = 17f
            setTextColor(0xFF222222.toInt())
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.05f)
        }
        card.addView(description, LinearLayout.LayoutParams(-1, dp(62)).apply {
            topMargin = dp(6)
            bottomMargin = dp(18)
        })

        val login = premiumButton("  👤   Login / Sign Up", true)
        card.addView(login, LinearLayout.LayoutParams(-1, dp(58)).apply {
            bottomMargin = dp(14)
        })
        login.setOnClickListener { openLogin() }

        val guest = premiumButton("  🛒   Continue as Guest", false)
        card.addView(guest, LinearLayout.LayoutParams(-1, dp(58)))
        guest.setOnClickListener { enterGuest() }

        setContentView(root)
    }

    private fun premiumButton(label: String, filled: Boolean): TextView = TextView(this).apply {
        text = label
        textSize = 18f
        gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setTextColor(if (filled) Color.WHITE else 0xFF16131D.toInt())
        background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            if (filled) intArrayOf(0xFF7135D4.toInt(), 0xFF6F35C9.toInt())
            else intArrayOf(Color.WHITE, Color.WHITE)
        ).apply {
            cornerRadius = dp(20).toFloat()
            if (!filled) setStroke(dp(2), 0xFFC0A7DF.toInt())
        }
        isClickable = true
        isFocusable = true
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(.985f).scaleY(.985f).setDuration(70).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
            }
            false
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)

    private fun enterGuest() {
        prefs().edit()
            .putBoolean("onboarded", true)
            .putBoolean("guest_mode", true)
            .putBoolean("signed_in", false)
            .putString("user_role", "BUYER")
            .remove("auth_token")
            .apply()
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun openLogin() {
        prefs().edit()
            .putBoolean("onboarded", true)
            .putBoolean("guest_mode", false)
            .putBoolean("signed_in", false)
            .apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java))
        finish()
    }
}
