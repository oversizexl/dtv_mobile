pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    // Mirrors as fallback for local networks where official repositories are unreliable.
    maven("https://maven.aliyun.com/repository/google")
    maven("https://maven.aliyun.com/repository/gradle-plugin")
    maven("https://maven.aliyun.com/repository/public")
  }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    maven("https://maven.aliyun.com/repository/google")
    maven("https://maven.aliyun.com/repository/public")
  }
}

rootProject.name = "DTV-mobile"

include(":shared")
include(":androidApp")
