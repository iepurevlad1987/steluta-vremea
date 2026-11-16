plugins {
    id("com.android.application")
}

android {
    namespace = "ro.iepur.agenda"
    compileSdk = 37

    defaultConfig {
        applicationId = "ro.iepur.agenda"
        // 31 (Android 12): lista din widget se face cu RemoteCollectionItems, fara serviciu.
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    // O cheie fixa, in repo: fiecare build de pe GitHub semneaza la fel, deci un APK nou
    // se instaleaza peste cel vechi fara dezinstalare. E o cheie de debug, nu de magazin.
    signingConfigs {
        getByName("debug") {
            storeFile = file("agenda.keystore")
            storePassword = "android"
            keyAlias = "agenda"
            keyPassword = "android"
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
