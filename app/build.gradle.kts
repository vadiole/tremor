plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "vadiole.tremor"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "vadiole.tremor"
        minSdk = 31
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
        base.archivesName.set("tremor-v$versionName")
    }
    androidResources {
        localeFilters += setOf("en", "cs", "da", "de", "el", "es", "fr", "hi", "it", "ja", "ka", "nb", "nl", "pl", "ru", "sl", "sv", "tr", "uk", "zh-rCN")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles("proguard-rules.pro")
        }

        create("beta") {
            initWith(buildTypes.getByName("debug"))
            applicationIdSuffix = ".beta"
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles("proguard-rules.pro")
        }
    }
    packaging {
        dex {
            useLegacyPackaging = true
        }
        resources {
            excludes += "kotlin/**"
            excludes += "DebugProbesKt.bin"
            excludes += "META-INF/**"
        }
    }
}
