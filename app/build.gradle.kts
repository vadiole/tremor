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
        versionCode = 3
        versionName = "1.2"
        base.archivesName.set("tremor-v$versionName")
    }
    androidResources {
        localeFilters += setOf(
            "en",
            "bn",
            "cs",
            "da",
            "de",
            "el",
            "es",
            "fr",
            "hi",
            "id",
            "in",
            "it",
            "ja",
            "ka",
            "nb",
            "nl",
            "pl",
            "pt-rBR",
            "ro",
            "ru",
            "sl",
            "sv",
            "th",
            "tr",
            "uk",
            "vi",
            "zh-rCN"
        )
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
