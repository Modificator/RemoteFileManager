import java.util.Properties
import java.io.InputStreamReader
import java.io.FileInputStream
import java.io.File

plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdk = 32

    defaultConfig {
        minSdk = 16
        targetSdk = 32

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    splits{
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.5.0")
    implementation("com.google.android.material:material:1.6.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}

fun Project.getLocalProperty(key: String, file: String = "local.properties"): Any {
    val properties = Properties()
    val localProperties = File(file)
    if (localProperties.isFile) {
        InputStreamReader(FileInputStream(localProperties), Charsets.UTF_8).use { reader ->
            properties.load(reader)
        }
    } else error("File from not found")

    return properties.getProperty(key)
}


tasks.register("buildGolang") {
    doFirst {
        val ndkDir = getLocalProperty("ndk.dir") as String
        val goDir = getLocalProperty("go.dir") as String
        exec {
            commandLine("$goDir/bin/go", "build","-buildmode=c-shared","-ldflags", "-s -w", "-o", "../jniLibs/armeabi-v7a/libFileServer.so")
                .workingDir("$projectDir/src/main/gofi-backend")
                .environment(
                    "GOOS" to "android",
                    "GOARCH" to "arm",
                    "CC" to "$ndkDir/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi19-clang",
                    "CXX" to "$ndkDir/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi19-clang++",
                    "CGO_ENABLED" to "1",
                    "GOARM" to "7",
                    "CGO_CPPFLAGS" to "",
                    "CGO_LDFLAGS" to "",
                )
        }
        exec {
            commandLine("$goDir/bin/go", "build", "-buildmode=c-shared", "-ldflags", "-s -w", "-o", "../jniLibs/arm64-v8a/libFileServer.so")
                .workingDir("$projectDir/src/main/gofi-backend")
                .environment(
                    "GOOS" to "android",
                    "GOARCH" to "arm64",
                    "CC" to "$ndkDir/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android21-clang",
                    "CXX" to "$ndkDir/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android21-clang++",
                    "CGO_ENABLED" to "1",
                    "GOARM" to "7",
                    "CGO_CPPFLAGS" to "",
                    "CGO_LDFLAGS" to "",
                )
        }
    }
    onlyIf {
        true
    }
}


tasks.register("buildWeb") {
    doFirst {
        exec {
            commandLine( "/usr/bin/yarn")
                .workingDir("$projectDir/src/main/gofi-frontend")
        }
        exec {
            commandLine( "/usr/bin/yarn", "build", "--mode","production")
                .workingDir("$projectDir/src/main/gofi-frontend")
        }
    }
    doLast {
        exec { commandLine("/usr/bin/rm", "-rf", "$projectDir/src/main/assets/*") }
        file("$projectDir/src/main/gofi-frontend/dist/").renameTo(File("$projectDir/src/main/assets"))
    }
    onlyIf {
        true
    }
}

afterEvaluate {
    tasks.forEach {
        if (it.name == "preBuild") {
            it.setDependsOn(listOf(tasks["buildGolang"], tasks["buildWeb"]))
        }
    }
}
