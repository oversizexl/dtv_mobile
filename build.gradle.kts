plugins {
  // Keep versions centralized here for a small repo.
  // Compose Multiplatform: https://github.com/JetBrains/compose-multiplatform
  kotlin("multiplatform") version "2.0.21" apply false
  kotlin("android") version "2.0.21" apply false
  id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
  id("org.jetbrains.compose") version "1.6.11" apply false
  id("com.android.application") version "8.5.2" apply false
  id("com.android.library") version "8.5.2" apply false
  kotlin("plugin.serialization") version "2.0.21" apply false
}
