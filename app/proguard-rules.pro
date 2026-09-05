# Keep Razorpay checkout classes referenced by the payment SDK at runtime.
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**

# Keep Parcelable/model constructors used by Android reflection.
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
