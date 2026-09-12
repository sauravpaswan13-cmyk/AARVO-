package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
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
            setPadding(30, 30, 30, 22)
            setBackgroundColor(Color.WHITE)
        }

        // Premium AARVO signature: logo + wordmark.
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aarvo_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "AARVO logo"
        }
        brand.addView(logo, LinearLayout.LayoutParams(76, 76))
        brand.addView(TextView(this).apply {
            text = "AARVO"
            textSize = 36f
            setTextColor(Color.rgb(31, 27, 110))
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = .035f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 0, 0, 0)
        }, LinearLayout.LayoutParams(-2, 76))
        root.addView(brand)

        root.addView(TextView(this).apply {
            text = "Your One Stop Shopping Destination"
            textSize = 16f
            setTextColor(Color.rgb(28, 32, 70))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }, LinearLayout.LayoutParams(-1, 48))

        // Large clean shopping-bag artwork, matching the supplied reference composition.
        root.addView(ShoppingBagsView(this), LinearLayout.LayoutParams(-1, 390).apply {
            topMargin = 12
            bottomMargin = 2
        })

        // Entry does not require verification. Login remains available inside the app.
        val continueShopping = Button(this).apply {
            text = "🛍   Continue Shopping"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = GradientDrawable().apply {
                cornerRadius = 28f
                setColor(Color.rgb(96, 31, 235))
            }
            setPadding(12, 0, 12, 0)
            setOnClickListener { enterApp() }
        }
        root.addView(continueShopping, LinearLayout.LayoutParams(-1, 72).apply { bottomMargin = 14 })

        val login = Button(this).apply {
            text = "🔒   Login / Sign Up"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(76, 34, 196))
            isAllCaps = false
            background = GradientDrawable().apply {
                cornerRadius = 28f
                setColor(Color.WHITE)
                setStroke(3, Color.rgb(102, 43, 230))
            }
            setPadding(12, 0, 12, 0)
            setOnClickListener { openLogin() }
        }
        root.addView(login, LinearLayout.LayoutParams(-1, 72).apply { bottomMargin = 12 })

        root.addView(TextView(this).apply {
            text = "Shop freely now • Login whenever you need"
            textSize = 14f
            setTextColor(Color.rgb(81, 34, 201))
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.NORMAL)
        }, LinearLayout.LayoutParams(-1, 42))

        val features = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 0)
        }
        val items = listOf("✓\nSecure\nShopping", "▣\nFast\nDelivery", "♧\n24/7\nSupport", "☆\nBest\nPrices")
        items.forEach { item ->
            features.addView(TextView(this).apply {
                text = item
                textSize = 12f
                setTextColor(Color.rgb(35, 31, 103))
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.NORMAL)
            }, LinearLayout.LayoutParams(0, 82, 1f))
        }
        root.addView(features, LinearLayout.LayoutParams(-1, 92))

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

    private class ShoppingBagsView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val cx = w / 2f
            val base = h * .86f

            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(248, 247, 255)
            canvas.drawOval(cx - w * .36f, h * .14f, cx + w * .36f, h * .86f, paint)

            drawBag(canvas, cx - w * .23f, base - 18, w * .27f, h * .52f, Color.rgb(255, 180, 10), Color.rgb(255, 222, 60), true)
            drawBag(canvas, cx + w * .02f, base - 12, w * .25f, h * .55f, Color.rgb(241, 22, 117), Color.rgb(255, 86, 150), true)
            drawBag(canvas, cx + w * .24f, base - 5, w * .19f, h * .44f, Color.rgb(82, 25, 210), Color.rgb(124, 66, 240), true)

            paint.color = Color.rgb(245, 35, 105)
            canvas.drawCircle(cx - w * .34f, h * .40f, 28f, paint)
            paint.color = Color.WHITE
            paint.textSize = 32f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("%", cx - w * .34f - 15, h * .40f + 11, paint)

            paint.color = Color.rgb(111, 58, 231)
            canvas.drawRoundRect(cx + w * .32f - 35, h * .32f, cx + w * .32f + 35, h * .32f + 70, 18f, 18f, paint)
            paint.color = Color.WHITE
            paint.textSize = 34f
            canvas.drawText("♡", cx + w * .32f - 18, h * .32f + 45, paint)
        }

        private fun drawBag(canvas: Canvas, x: Float, bottom: Float, bagW: Float, bagH: Float, body: Int, top: Int, handle: Boolean) {
            val left = x - bagW / 2f
            val right = x + bagW / 2f
            val topY = bottom - bagH
            paint.style = Paint.Style.FILL
            paint.color = body
            canvas.drawRoundRect(left, topY + 28, right, bottom, 10f, 10f, paint)

            paint.color = top
            canvas.drawRect(left, topY + 28, right, topY + 58, paint)
            paint.color = Color.argb(35, 255, 255, 255)
            canvas.drawRect(left + bagW * .18f, topY + 28, left + bagW * .25f, bottom - 5, paint)
            canvas.drawRect(left + bagW * .52f, topY + 28, left + bagW * .59f, bottom - 5, paint)

            if (handle) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 10f
                paint.strokeCap = Paint.Cap.ROUND
                paint.color = top
                val path = Path()
                path.moveTo(x - bagW * .28f, topY + 38)
                path.cubicTo(x - bagW * .26f, topY - 65, x + bagW * .26f, topY - 65, x + bagW * .28f, topY + 38)
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }
}
