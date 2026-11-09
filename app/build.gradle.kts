plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// A chave privada nunca deve ser salva no repositório.
// O workflow do GitHub disponibiliza estes valores somente durante a compilação.
val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_FILE")
val releaseStorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")

android {
    namespace = "br.com.glicozinho.app"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "br.com.glicozinho.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "1.0.7"
    }

    signingConfigs {
        create("release") {
            storeFile = releaseKeystorePath?.let { file(it) }
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
            storeType = "PKCS12"
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
