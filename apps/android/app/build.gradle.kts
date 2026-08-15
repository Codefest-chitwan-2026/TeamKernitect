plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    /*
     * Room code generation.
     */
    alias(libs.plugins.ksp)
}

android {

    namespace =
        "com.kernitect.saharaandroid"

    compileSdk {
        version =
            release(37)
    }

    defaultConfig {

        applicationId =
            "com.kernitect.saharaandroid"

        minSdk =
            30

        targetSdk =
            37

        versionCode =
            1

        versionName =
            "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        release {

            optimization {
                enable =
                    false
            }
        }
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    buildFeatures {

        compose =
            true
    }
}

dependencies {

    /*
     * -------------------------
     * COMPOSE
     * -------------------------
     */

    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )


    /*
     * -------------------------
     * LOCATION
     * -------------------------
     */

    implementation(
        "com.google.android.gms:play-services-location:21.4.0"
    )


    /*
     * -------------------------
     * MATERIAL ICONS
     * -------------------------
     */

    implementation(
        "androidx.compose.material:material-icons-extended"
    )


    /*
     * -------------------------
     * MAP
     * -------------------------
     */

    implementation(
        "org.osmdroid:osmdroid-android:6.1.20"
    )


    /*
     * -------------------------
     * ROOM LOCAL DATABASE
     * -------------------------
     *
     * Used for:
     *
     * - received incidents
     * - notifications
     * - witness reports
     * - tracking events
     * - public disaster alerts
     *
     * Everything will remain available
     * without internet.
     */

    implementation(
        libs.androidx.room.runtime
    )

    ksp(
        libs.androidx.room.compiler
    )


    /*
     * -------------------------
     * TESTING
     * -------------------------
     */

    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}