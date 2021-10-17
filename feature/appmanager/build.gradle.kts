plugins {
    id ("com.android.library")
    kotlin(BuildPlugins.kotlinAndroid)
    kotlin(BuildPlugins.kotlinKapt)
    id("androidx.navigation.safeargs")
    id("dagger.hilt.android.plugin")
}

android {

    defaultConfig {
        minSdk = AndroidSdk.min
        compileSdk = AndroidSdk.compile
        targetSdk = AndroidSdk.target
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    kotlinOptions.apply {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    compileOptions.apply {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
//        dataBinding = true
    }
    lint {
        isAbortOnError = false
        lintConfig = File("${project.rootDir}/lint/lint-config.xml")
        htmlOutput = File("${project.rootDir}/lint/lint-report.html")
    }
}

dependencies {
    implementation(project(":common"))

    implementation(Libraries.Kotlin.stdlib)
    implementation(Libraries.Kotlin.coroutineCore)
    implementation(Libraries.Kotlin.coroutineAndroid)

    implementation(Libraries.viewModelKtx)
    implementation(Libraries.lifecycleLiveData)
    implementation(Libraries.lifecycleRuntime)
    implementation(Libraries.constraintLayout)
    implementation(Libraries.activityKtx)
    implementation(Libraries.fragmentKtx)
    implementation(Libraries.swipeRefresh)
    kapt(Libraries.lifecycleCompiler)
    implementation(Libraries.hilt)
    implementation(Libraries.navigation)
    implementation(Libraries.navigationUi)
    implementation(Libraries.navFeature)
    implementation(Libraries.preference)
    kapt(Libraries.hiltCompiler)
    kapt(Libraries.annotation)

    implementation(Libraries.exif)
    implementation(Libraries.appCompat)
    implementation(Libraries.recyclerView)
    implementation(Libraries.design)

    implementation(Libraries.palette)

    implementation(Libraries.ExternalLibs.glideRuntime)
    kapt(Libraries.ExternalLibs.glideCompiler)
    implementation(Libraries.ExternalLibs.fastScrollRecyclerview)

}