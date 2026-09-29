plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.aarvo"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.aarvo"
        minSdk = 24
        targetSdk = 37
        versionCode = 10
        versionName = "1.9"
        fun buildConfigString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n") + "\""
        val apiBaseUrl = project.findProperty("aarvoApiBaseUrl")?.toString() ?: "https://aarvo-api.onrender.com"
        val widgetId = project.findProperty("msg91WidgetId")?.toString() ?: "366968715030323230313530"
        val widgetToken = project.findProperty("msg91WidgetToken")?.toString() ?: ""
        // Firebase configuration is sourced from app/google-services.json when present.
        val firebaseConfigFile = rootProject.file("app/google-services.json")
        val firebaseConfig = if (firebaseConfigFile.exists()) {
            @Suppress("UNCHECKED_CAST")
            groovy.json.JsonSlurper().parse(firebaseConfigFile) as Map<String, Any?>
        } else emptyMap()
        val firebaseProjectInfo = (firebaseConfig["project_info"] as? Map<*, *>) ?: emptyMap<String, Any?>()
        val firebaseClient = ((firebaseConfig["client"] as? List<*>)?.firstOrNull() as? Map<*, *>) ?: emptyMap<String, Any?>()
        val firebaseClientInfo = (firebaseClient["client_info"] as? Map<*, *>) ?: emptyMap<String, Any?>()
        val firebaseAndroidInfo = (firebaseClientInfo["android_client_info"] as? Map<*, *>) ?: emptyMap<String, Any?>()
        val firebaseApiKeyList = (firebaseClient["api_key"] as? List<*>) ?: emptyList<Any?>()
        val firebaseApiKeyMap = (firebaseApiKeyList.firstOrNull() as? Map<*, *>) ?: emptyMap<String, Any?>()
        val firebaseApiKey = project.findProperty("firebaseApiKey")?.toString()
            ?: firebaseApiKeyMap["current_key"]?.toString().orEmpty()
        val firebaseProjectId = project.findProperty("firebaseProjectId")?.toString()
            ?: firebaseProjectInfo["project_id"]?.toString().orEmpty()
        val firebaseAppId = project.findProperty("firebaseAppId")?.toString()
            ?: firebaseClientInfo["mobilesdk_app_id"]?.toString().orEmpty()
        val firebaseSenderId = project.findProperty("firebaseSenderId")?.toString()
            ?: firebaseProjectInfo["project_number"]?.toString().orEmpty()
        val firebasePackageName = firebaseAndroidInfo["package_name"]?.toString().orEmpty()
        check(firebasePackageName.isBlank() || firebasePackageName == applicationId) {
            "Firebase package mismatch: expected " + applicationId + ", got " + firebasePackageName
        }
        buildConfigField("String", "AARVO_API_BASE_URL", buildConfigString(apiBaseUrl))
        buildConfigField("String", "MSG91_WIDGET_ID", buildConfigString(widgetId))
        buildConfigField("String", "MSG91_WIDGET_TOKEN", buildConfigString(widgetToken))
        buildConfigField("String", "FIREBASE_API_KEY", buildConfigString(firebaseApiKey))
        buildConfigField("String", "FIREBASE_PROJECT_ID", buildConfigString(firebaseProjectId))
        buildConfigField("String", "FIREBASE_APP_ID", buildConfigString(firebaseAppId))
        buildConfigField("String", "FIREBASE_SENDER_ID", buildConfigString(firebaseSenderId))
    }
    val releaseStoreFile = providers.gradleProperty("aarvoReleaseStoreFile").orNull
    val releaseStorePassword = providers.gradleProperty("aarvoReleaseStorePassword").orNull
    val releaseKeyAlias = providers.gradleProperty("aarvoReleaseKeyAlias").orNull
    val releaseKeyPassword = providers.gradleProperty("aarvoReleaseKeyPassword").orNull
    val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }
    signingConfigs { if (hasReleaseSigning) { create("production") { storeFile = file(releaseStoreFile!!); storePassword = releaseStorePassword; keyAlias = releaseKeyAlias; keyPassword = releaseKeyPassword } } }
    buildTypes {
        getByName("release") {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("production")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach { compilerOptions.freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api") }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.razorpay:checkout:1.6.41")
    implementation("com.msg91.lib:sendotp:1.0.0")
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
