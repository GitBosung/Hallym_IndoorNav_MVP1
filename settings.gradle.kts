pluginManagement {
    repositories {
        google()
        mavenCentral()  // ✅ TensorFlow Lite는 Maven Central에서 제공됨
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://storage.googleapis.com/download.tensorflow.org/maven")  // 수정된 구문
        }
    }
}

rootProject.name = "Hallym_indoor_nav_MVP"
include(":app")
