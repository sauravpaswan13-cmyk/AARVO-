package com.aarvo

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
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
            setPadding(dp(24), dp(28), dp(24), dp(24))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(27, 0, 88),
                    Color.rgb(69, 18, 178),
                    Color.rgb(167, 31, 183),
                    Color.rgb(30, 73, 180)
                )
            )
        }

        root.addView(Space(this), LinearLayout.LayoutParams(1, 0, 0.08f))

        root.addView(TextView(this).apply {
            text = "AARVO"
            textSize = 46f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = .075f
            setShadowLayer(dp(14).toFloat(), 0f, dp(4).toFloat(), Color.argb(170, 0, 0, 0))
        }, LinearLayout.LayoutParams(-1, dp(66)))

        root.addView(TextView(this).apply {
            text = "YOUR ONE-STOP SHOPPING DESTINATION"
            textSize = 11f
            setTextColor(Color.rgb(255, 220, 110))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = .09f
        }, LinearLayout.LayoutParams(-1, dp(28)))

        val visual = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.argb(34, 255, 255, 255))
                cornerRadius = dp(34).toFloat()
                setStroke(dp(1), Color.argb(70, 255, 255, 255))
            }
            elevation = dp(8).toFloat()

            addView(TextView(this@WelcomeActivity).apply {
                text = "🛒"
                textSize = 82f
                gravity = Gravity.CENTER
                includeFontPadding = true
            }, LinearLayout.LayoutParams(-1, dp(124)))

            addView(TextView(this@WelcomeActivity).apply {
                text = "Everything you want.\nOne beautiful place."
                textSize = 15f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                alpha = .94f
            }, LinearLayout.LayoutParams(-1, dp(52)))
        }
        root.addView(visual, LinearLayout.LayoutParams(-1, 0, 0.48f).apply {
            topMargin = dp(18)
            bottomMargin = dp(18)
        })

        val guest = premiumButton(
            text = "Continue as Guest",
            fill = true,
            accent = Color.rgb(255, 215, 90)
        )
        guest.setOnClickListener { enterGuest() }
        root.addView(guest, LinearLayout.LayoutParams(-1, dp(58)).apply {
            bottomMargin = dp(13)
        })

        val login = premiumButton(
            text = "Login / Sign Up",
            fill = false,
            accent = Color.WHITE
        )
        login.setOnClickListener { openLogin() }
        root.addView(login, LinearLayout.LayoutParams(-1, dp(58)))

        root.addView(TextView(this).apply {
            text = "Tap an option to begin"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            alpha = .72f
        }, LinearLayout.LayoutParams(-1, dp(34)).apply {
            topMargin = dp(7)
        })

        root.addView(Space(this), LinearLayout.LayoutParams(1, 0, 0.07f))

        setContentView(root)
    }

    private fun premiumButton(text: String, fill: Boolean, accent: Int): Button {
        return Button(this).apply {
            this.text = text
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(if (fill) Color.rgb(63, 24, 135) else Color.WHITE)
            stateListAnimator = null
            isFocusable = true
            isClickable = true

            val normal = GradientDrawable().apply {
                cornerRadius = dp(19).toFloat()
                if (fill) {
                    setColor(Color.WHITE)
                    setStroke(dp(1), Color.argb(110, 255, 255, 255))
                } else {
                    setColor(Color.argb(28, 255, 255, 255))
                    setStroke(dp(2), accent)
                }
            }
            background = RippleDrawable(
                ColorStateList.valueOf(Color.argb(75, 255, 255, 255)),
                normal,
                null
            )

            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        view.animate().scaleX(.97f).scaleY(.97f).alpha(.86f).setDuration(70).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120).start()
                    }
                }
                false
            }
        }
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
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun openLogin() {
        prefs().edit().putBoolean("onboarded", true).putBoolean("guest_mode", false).apply()
        startActivity(Intent(this, PhoneAuthActivity::class.java))
        finish()
    }
}
