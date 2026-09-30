plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.keon9608.biblewidget"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.keon9608.biblewidget"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        getByName("debug") {
            // CI는 캐시해 둔 키로 서명해서, 새 APK를 지우지 않고 덮어 설치할 수 있게 한다.
            System.getenv("DEBUG_KEYSTORE")?.let { storeFile = file(it) }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
