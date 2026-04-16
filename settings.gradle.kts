pluginManagement {
  repositories {
    // Mirrors (helpful in regions where dl.google.com is unreliable)
    maven("https://maven.aliyun.com/repository/google")
    maven("https://maven.aliyun.com/repository/gradle-plugin")
    maven("https://maven.aliyun.com/repository/public")
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    maven("https://maven.aliyun.com/repository/google")
    maven("https://maven.aliyun.com/repository/public")
    google()
    mavenCentral()
  }
}

rootProject.name = "DTV-mobile"

include(":shared")
include(":androidApp")
