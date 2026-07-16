plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
}

android {
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
    }

    namespace = "br.com.ebook"

    testOptions {
        unitTests {
            all {
                it.useJUnitPlatform()
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Ebook e Parsing de Texto
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.github.albfernandez:juniversalchardet:2.5.0")
    implementation(files("libs/rtfparserkit-1.10.0.jar"))

    // Coroutines para I/O Assíncrono
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Log
    implementation("org.slf4j:slf4j-api:2.0.16")

    // Test
    implementation("androidx.core:core-ktx:1.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.slf4j:slf4j-nop:2.0.16")
}
