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
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

class WelcomeActivity : ComponentActivity() {
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(248, 247, 252)) }

        val topBand = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(91,33,214), Color.rgb(255,85,215), Color.rgb(85,200,255), Color.rgb(255,212,92))
            )
        }
        root.addView(topBand, FrameLayout.LayoutParams(-1, dp(5), Gravity.TOP))

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(44), dp(24), dp(30))
        }

        val mark = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_top_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            contentDescription = "AARVO logo"
        }
        content.addView(mark, LinearLayout.LayoutParams(dp(104), dp(104)).apply {
            bottomMargin = dp(10)
        })

        val name = TextView(this).apply {
            text = "AARVO"
            setTextColor(Color.rgb(91,33,214))
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = .08f
            gravity = Gravity.CENTER
            includeFontPadding = true
        }
        content.addView(name, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(22)
        })

        val title = TextView(this).apply {
            text = "Welcome"
            setTextColor(Color.rgb(28,24,34))
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            includeFontPadding = true
        }
        content.addView(title, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })

        val subtitle = TextView(this).apply {
            text = "Discover products you love, all in one place."
            setTextColor(Color.rgb(98,91,109))
            textSize = 16f
            gravity = Gravity.CENTER
            includeFontPadding = true
            maxLines = 2
            setLineSpacing(0f, 1.1f)
        }
        content.addView(subtitle, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(18)
        })

        val divider = View(this).apply { setBackgroundColor(Color.rgb(226,221,234)) }
        content.addView(divider, LinearLayout.LayoutParams(dp(170), dp(1)).apply {
            bottomMargin = dp(22)
        })

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        fun button(label: String, filled: Boolean): TextView = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            includeFontPadding = true
            setTextColor(if (filled) Color.WHITE else Color.rgb(91,33,214))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                if (filled) setColor(Color.rgb(91,33,214))
                else {
                    setColor(Color.WHITE)
                    setStroke(dp(2), Color.rgb(91,33,214))
                }
            }
            elevation = dp(2).toFloat()
            isClickable = true
            isFocusable = true
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(.98f).scaleY(.98f).setDuration(70).start()
                        false
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                        false
                    }
                    else -> false
                }
            }
        }

        val guest = button("Continue as Guest", true)
        guest.setOnClickListener { enterGuest() }
        actions.addView(guest, LinearLayout.LayoutParams(-1, dp(54)).apply {
            bottomMargin = dp(14)
        })

        val login = button("Login / Sign Up", false)
        login.setOnClickListener { openLogin() }
        actions.addView(login, LinearLayout.LayoutParams(-1, dp(54)))
        content.addView(actions, LinearLayout.LayoutParams(-1, -2))

        val footer = TextView(this).apply {
            text = "AARVO"
            setTextColor(Color.rgb(150,143,160))
            textSize = 11f
            letterSpacing = .18f
            gravity = Gravity.CENTER
            includeFontPadding = true
        }
        content.addView(footer, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(18)
        })

        scroll.addView(content, ViewGroup.LayoutParams(-1, -1))
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1).apply {
            topMargin = dp(5)
        })
        setContentView(root)
    }

    private fun prefs() = getSharedPreferences("aarvo_prefs", Context.MODE_PRIVATE)

    private fun enterGuest() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", true)
            .putBoolean("signed_in", false).putString("user_role", "BUYER")
            .remove("auth_token").apply()
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun openLogin() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", false)
            .putBoolean("signed_in", false).apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java))
        finish()
    }
}
