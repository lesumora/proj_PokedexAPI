plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
<<<<<<< HEAD
    namespace = "com.validex.madproject"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.validex.madproject"
        minSdk = 24
        targetSdk = 35
=======
    namespace = "com.example.proj_pokedexapi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.proj_pokedexapi"
        minSdk = 26
        targetSdk = 34
>>>>>>> 2ce15fcaec9ee7f442102de1cc160fc350040bcb
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
<<<<<<< HEAD

=======
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.firebase.auth)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
>>>>>>> 2ce15fcaec9ee7f442102de1cc160fc350040bcb
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
<<<<<<< HEAD
    implementation(libs.firebase.auth)
=======
>>>>>>> 2ce15fcaec9ee7f442102de1cc160fc350040bcb
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}