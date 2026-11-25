plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.bharatai.learningcompanion" // ध्यान दें: इसे अपने प्रोजेक्ट के असली namespace/package नाम के अनुसार ही रखें
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bharatai.learningcompanion" // इसे भी अपने प्रोजेक्ट के असली applicationId के अनुसार रखें
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // केवल Arm64 प्रोसेसर के लिए (मॉडल स्पीड और कम साइज़ के लिए ज़रूरी)
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // नेटिव C++ लाइब्रेरी (ExecuTorch) के पैकेजिंग टकराव से बचने के लिए
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {implementation("org.pytorch:executorch-android:0.4.0")

    // Android Core और UI लाइब्रेरी
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // 1. PyTorch ExecuTorch Android लाइब्रेरी (मुख्य ऑफलाइन AI इंजन)
    implementation("org.pytorch:executorch-android:0.4.0")

    // 2. Kotlin Coroutines (AI को बैकग्राउंड में चलाने के लिए ताकि ऐप हैंग न हो)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // 3. Room Database (स्टडी प्लान, चैट हिस्ट्री और क्विज़ स्कोर ऑफलाइन सेव करने के लिए)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // टेस्टिंग लाइब्रेरी
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
