plugins { id("com.android.application") }

android {
    namespace = "com.rockjazz.metronome"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rockjazz.metronome"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "2.5.0"
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
