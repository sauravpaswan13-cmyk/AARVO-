package com.aarvo

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

private class AarvoWelcomeLogoView(context: android.content.Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE; strokeWidth=18f; strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND }
    override fun onDraw(canvas: Canvas) {
        val w=width.toFloat(); val h=height.toFloat()
        paint.shader=LinearGradient(0f,h,w,0f,intArrayOf(Color.rgb(255,174,0),Color.rgb(255,104,31),Color.rgb(235,38,111),Color.rgb(73,42,196)),null,Shader.TileMode.CLAMP)
        val p=Path().apply{moveTo(w*.18f,h*.84f);lineTo(w*.50f,h*.10f);lineTo(w*.84f,h*.84f)}
        canvas.drawPath(p,paint)
        paint.strokeWidth=14f; canvas.drawLine(w*.30f,h*.56f,w*.70f,h*.56f,paint)
    }
}

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(28,34,28,28);setBackgroundColor(Color.WHITE)}
        root.addView(AarvoWelcomeLogoView(this),LinearLayout.LayoutParams(120,100).apply{gravity=Gravity.CENTER})
        val title=TextView(this).apply{text="AARVO";textSize=38f;setTextColor(Color.rgb(40,31,132));gravity=Gravity.CENTER;setTypeface(typeface,Typeface.BOLD)}
        root.addView(title,LinearLayout.LayoutParams(-1,-2))
        val subtitle=TextView(this).apply{text="Your One Stop Shopping Destination";textSize=15f;setTextColor(Color.rgb(80,80,105));gravity=Gravity.CENTER;setPadding(0,5,0,0)}
        root.addView(subtitle,LinearLayout.LayoutParams(-1,-2))
        val bags=TextView(this).apply{text="🛍️   🛍️   🛍️";textSize=30f;gravity=Gravity.CENTER;setPadding(0,24,0,26)}
        root.addView(bags,LinearLayout.LayoutParams(-1,-2))
        val guest=Button(this).apply{text="Continue as Guest";textSize=16f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.WHITE);background=android.graphics.drawable.GradientDrawable().apply{cornerRadius=18f;setColor(Color.rgb(83,34,211))};setOnClickListener{enterGuest()}}
        root.addView(guest,LinearLayout.LayoutParams(-1,56).apply{bottomMargin=12})
        val login=Button(this).apply{text="Login / Sign Up";textSize=16f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(65,35,170));background=android.graphics.drawable.GradientDrawable().apply{cornerRadius=18f;setColor(Color.WHITE);setStroke(3,Color.rgb(105,56,222))};setOnClickListener{openLogin()}}
        root.addView(login,LinearLayout.LayoutParams(-1,56).apply{bottomMargin=10})
        val explore=TextView(this).apply{text="Explore as Guest";textSize=15f;setTextColor(Color.rgb(72,35,176));gravity=Gravity.CENTER;setTypeface(typeface,Typeface.BOLD);setPadding(0,10,0,18);setOnClickListener{enterGuest()}}
        root.addView(explore,LinearLayout.LayoutParams(-1,-2))
        val trust=TextView(this).apply{text="Secure Shopping  •  Trusted Support  •  Fast Delivery";textSize=12f;setTextColor(Color.rgb(102,96,112));gravity=Gravity.CENTER}
        root.addView(trust,LinearLayout.LayoutParams(-1,-2))
        setContentView(root)
    }
    private fun prefs()=getSharedPreferences("aarvo_prefs",Context.MODE_PRIVATE)
    private fun enterGuest(){prefs().edit().putBoolean("onboarded",true).putBoolean("guest_mode",true).putBoolean("signed_in",false).apply();startActivity(Intent(this,MainActivity::class.java));finish()}
    private fun openLogin(){prefs().edit().putBoolean("onboarded",true).putBoolean("guest_mode",false).apply();startActivity(Intent(this,PhoneAuthActivity::class.java));finish()}
}
