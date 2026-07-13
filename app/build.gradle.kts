plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

android {
    namespace = "com.aiide"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aiide.editor"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // WebView 代码编辑器 (Monaco Editor)
    implementation("androidx.webkit:webkit:1.9.0")
    
    // 网络请求 (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON解析
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // ViewPager2 (配合 TabLayout)
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // 文件浏览
    implementation("androidx.documentfile:documentfile:1.0.1")

    // 协程生命周期集成
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}