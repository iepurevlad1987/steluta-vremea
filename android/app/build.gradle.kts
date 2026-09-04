// Un singur plugin: de la AGP 9, el aduce si compilatorul Kotlin. Vezi build.gradle.kts
// din radacina.
plugins {
    id("com.android.application")
}

android {
    namespace = "ro.iepur.steluta"
    compileSdk = 37

    defaultConfig {
        applicationId = "ro.iepur.steluta"
        // 26 (Android 8) e minimul la care widget-urile cu vector drawables se comporta
        // predictibil. Sub asta ar trebui iconite PNG in patru densitati, pentru telefoane
        // pe care oricum nu ruleaza nimeni.
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            // Fara minificare: aplicatia are sub 1000 de linii si nicio reflectie.
            // R8 n-ar avea ce sa taie, dar ar putea sa strice.
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// **Nicio dependinta.** Reteaua se face cu HttpURLConnection, JSON-ul cu org.json, si
// widget-ul cu RemoteViews - toate sunt in Android. Fara Retrofit, Moshi, Compose sau
// Hilt nu exista nimic care sa se strice la un update de bibliotecă.
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
