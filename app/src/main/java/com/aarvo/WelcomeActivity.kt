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

        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(18, 8, 46), Color.rgb(56, 14, 96), Color.rgb(12, 25, 74))
            )
        }

        val glow = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x2855C8FF.toInt())
            }
            alpha = .8f
        }
        root.addView(glow, FrameLayout.LayoutParams(dp(360), dp(360), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(62)
        })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(72), dp(28), dp(30))
        }

        val mark = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_entry_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "AARVO logo"
            elevation = dp(8).toFloat()
        }
        content.addView(mark, LinearLayout.LayoutParams(dp(122), dp(122)).apply {
            bottomMargin = dp(12)
        })

        val name = TextView(this).apply {
            text = "AARVO"
            setTextColor(Color.WHITE)
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = .10f
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        content.addView(name, LinearLayout.LayoutParams(-1, dp(46)).apply {
            bottomMargin = dp(18)
        })

        val title = TextView(this).apply {
            text = "Welcome"
            setTextColor(Color.WHITE)
            textSize = 27f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        content.addView(title, LinearLayout.LayoutParams(-1, dp(38)).apply {
            bottomMargin = dp(8)
        })

        val subtitle = TextView(this).apply {
            text = "Shop what you love.\nSimple, secure and made for you."
            setTextColor(0xD9FFFFFF.toInt())
            textSize = 15f
            gravity = Gravity.CENTER
            includeFontPadding = false
            setLineSpacing(dp(2).toFloat(), 1.0f)
        }
        content.addView(subtitle, LinearLayout.LayoutParams(-1, dp(48)).apply {
            bottomMargin = dp(34)
        })

        fun button(label: String, filled: Boolean): TextView = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            includeFontPadding = false
            setTextColor(if (filled) Color.rgb(35, 14, 66) else Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(17).toFloat()
                if (filled) {
                    setColor(Color.WHITE)
                } else {
                    setColor(0x1AFFFFFF)
                    setStroke(dp(1), 0xBFFFFFFF.toInt())
                }
            }
            elevation = dp(5).toFloat()
            isClickable = true
            isFocusable = true
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(.97f).scaleY(.97f).setDuration(70).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                }
                false
            }
        }

        val guest = button("Continue as Guest", true)
        guest.setOnClickListener { enterGuest() }
        content.addView(guest, LinearLayout.LayoutParams(-1, dp(56)).apply {
            bottomMargin = dp(14)
        })

        val login = button("Login / Sign Up", false)
        login.setOnClickListener { openLogin() }
        content.addView(login, LinearLayout.LayoutParams(-1, dp(56)))

        val footer = TextView(this).apply {
            text = "AARVO"
            setTextColor(0x8FFFFFFF.toInt())
            textSize = 10f
            letterSpacing = .22f
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        content.addView(footer, LinearLayout.LayoutParams(-1, dp(20)).apply {
            topMargin = dp(30)
        })

        scroll.addView(content, ViewGroup.LayoutParams(-1, -1))
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
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
