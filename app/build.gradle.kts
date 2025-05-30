
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)

    

}


android {
    namespace = "com.example.homeservice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.homeservice"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }


}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.base)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.location)
    implementation(libs.firebase.messaging)
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation ("com.google.android.gms:play-services-auth:21.0.0") //boton google
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.squareup.picasso:picasso:2.71828")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation ("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
    implementation ("com.google.firebase:firebase-storage:20.3.0")
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("androidx.activity:activity:1.8.2")
    implementation (platform("com.google.firebase:firebase-bom:33.12.0"))
    //implementation ("com.google.firebase:firebase-messaging:20.2.0")
    // Opcional: Analytics (recomendado para informes de entrega)
    implementation ("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-functions-ktx")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("androidx.preference:preference:1.2.0")






}





