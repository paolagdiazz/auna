plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.auna.padres"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.auna.padres"
        minSdk = 26
        targetSdk = 37
        versionCode = 7
        versionName = "0.6"
    }

    buildTypes {
        release {
            // R8 reduce el APK (el SDK de Anthropic trae sus propias reglas) y lo hace más rápido.
            optimization {
                enable = true
                keepRules {
                    files.add(file("proguard-rules.pro"))
                }
            }
            // Versión de prueba: firmada con la clave de desarrollo para poder instalarla
            // encima de la anterior. Para Play Store hará falta una clave propia.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization)
    implementation(libs.anthropic.java)
    implementation(libs.zxing.core)
    implementation(libs.androidx.print)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
