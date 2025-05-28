
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")

}

configurations.all {
    exclude(group = "xmlpull", module = "xmlpull")
    exclude(group = "xpp3", module = "xpp3_min")
}
android {
    namespace = "com.elena.autoplanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.elena.autoplanner"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] =
            "ACTIVITY-MISSING,EMULATOR,LOW-BATTERY,UNLOCKED,UNSUSTAINED-ACTIVITY,DEBUGGABLE"

        vectorDrawables {
            useSupportLibrary = true
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true

    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            isMinifyEnabled = true
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

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {

            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.ui)
    implementation(libs.gson)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)


    implementation(libs.androidx.room.runtime) 
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.jems2)
    implementation(libs.rfc5545.datetime)
    implementation(libs.lib.recur)
    implementation(libs.androidx.navigation.safe.args.generator) 
    implementation(libs.androidx.material3.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.foundation.layout.android)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.animation.core.android)
    implementation(libs.androidx.compose.testing) 
    implementation(libs.protolite.well.known.types)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m2)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.views)
    implementation("androidx.glance:glance-appwidget:1.1.0-beta01")
    implementation("androidx.glance:glance-material3:1.1.0-beta01")


    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test) 
    testImplementation(libs.turbine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)


    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.ext.junit.ktx)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.androidx.test.rules)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.espresso.core)
    testImplementation(libs.mockk.android)
    testImplementation(libs.truth)
    testImplementation(libs.mockito.kotlin)
    testImplementation("androidx.benchmark:benchmark-junit4:1.2.2")

    
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    testImplementation(kotlin("test"))
}