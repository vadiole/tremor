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
        versionCode = 1
        versionName = "1.0"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        resources {
            excludes += "kotlin/**"
            excludes += "DebugProbesKt.bin"
            excludes += "META-INF/*.version"
            excludes += "META-INF/services/**"
            excludes += "META-INF/version-control-info.textproto"
            excludes += "META-INF/com/**"
        }
    }
}
