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
import android.widget.FrameLayout
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
            setPadding(dp(24), dp(30), dp(24), dp(26))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(15, 8, 48),
                    Color.rgb(49, 20, 112),
                    Color.rgb(104, 38, 150),
                    Color.rgb(24, 43, 105)
                )
            )
        }

        root.addView(Space(this), LinearLayout.LayoutParams(1, 0, 0.06f))

        root.addView(TextView(this).apply {
            text = "AARVO"
            textSize = 44f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = .12f
            setShadowLayer(dp(14).toFloat(), 0f, dp(4).toFloat(), Color.argb(170, 0, 0, 0))
        }, LinearLayout.LayoutParams(-1, dp(66)))

        root.addView(TextView(this).apply {
            text = "PREMIUM SHOPPING • MADE FOR YOU"
            textSize = 11f
            setTextColor(Color.rgb(255, 220, 110))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = .09f
        }, LinearLayout.LayoutParams(-1, dp(28)))

        // Premium trolley scene: the trolley stays fixed while the market lane slides behind it.
        val visual = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.argb(34, 255, 255, 255))
                cornerRadius = dp(34).toFloat()
                setStroke(dp(1), Color.argb(70, 255, 255, 255))
            }
            elevation = dp(8).toFloat()
        }

        val lane = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }

        // Repeated shopping items create a continuous market ribbon behind the trolley.
        listOf("👕", "👜", "📱", "👟", "⌚", "🎧", "👕", "👜").forEach { item ->
            lane.addView(TextView(this@WelcomeActivity).apply {
                text = item
                textSize = 25f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.TRANSPARENT)
            }, LinearLayout.LayoutParams(dp(58), dp(58)).apply {
                leftMargin = dp(4)
                rightMargin = dp(4)
            })
        }

        visual.addView(lane, FrameLayout.LayoutParams(dp(560), dp(72), Gravity.CENTER_VERTICAL).apply {
            leftMargin = dp(-8)
        })

        // A fine horizontal motion line makes the sliding market trail visually clear.
        visual.addView(View(this).apply {
            setBackgroundColor(Color.argb(85, 255, 255, 255))
        }, FrameLayout.LayoutParams(dp(242), dp(1), Gravity.CENTER_VERTICAL).apply {
            leftMargin = dp(18)
            rightMargin = dp(18)
        })

        val trolley = TextView(this).apply {
            text = "🛒"
            textSize = 70f
            gravity = Gravity.CENTER
            includeFontPadding = true
            elevation = dp(10).toFloat()
            setShadowLayer(dp(8).toFloat(), 0f, dp(3).toFloat(), Color.argb(110, 0, 0, 0))
        }
        visual.addView(trolley, FrameLayout.LayoutParams(dp(118), dp(118), Gravity.CENTER).apply {
            leftMargin = dp(108)
        })

        val trail = TextView(this).apply {
            text = "AARVO  •  SHOP  •  DISCOVER"
            textSize = 10f
            setTextColor(Color.argb(155, 255, 220, 110))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        visual.addView(trail, FrameLayout.LayoutParams(dp(95), dp(32), Gravity.CENTER).apply {
            leftMargin = dp(26)
            topMargin = dp(48)
        })

        root.addView(visual, LinearLayout.LayoutParams(-1, dp(170)).apply {
            topMargin = dp(18)
            bottomMargin = dp(18)
        })

        // The lane moves back-and-forth behind the trolley; the trolley itself never changes position.
        lane.animate()
            .translationX(-dp(92).toFloat())
            .setDuration(1800)
            .withEndAction {
                lane.animate()
                    .translationX(0f)
                    .setDuration(1800)
                    .withEndAction { lane.animate().translationX(-dp(92).toFloat()).setDuration(1800).start() }
                    .start()
            }
            .start()

        trolley.animate()
            .rotation(-2.2f)
            .setDuration(650)
            .withEndAction {
                trolley.animate()
                    .rotation(2.2f)
                    .setDuration(650)
                    .withEndAction { trolley.animate().rotation(-2.2f).setDuration(650).start() }
                    .start()
            }
            .start()

        trail.animate()
            .alpha(.35f)
            .setDuration(500)
            .withEndAction { trail.animate().alpha(1f).setDuration(500).start() }
            .start()

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
            text = "WELCOME TO AARVO"
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
