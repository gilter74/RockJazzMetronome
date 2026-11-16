plugins { id("com.android.application") }

android {
    namespace = "com.rockjazz.metronome"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rockjazz.metrotuner"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "3.8.1"
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
}

dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
}
