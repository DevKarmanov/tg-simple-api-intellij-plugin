plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.21"
  id("org.jetbrains.intellij") version "1.16.1"
}

group = "dev.karmanov"
version = "1.0.2"

repositories {
  mavenCentral()
}

val ideaVersion = project.findProperty("idea.version")?.toString() ?: "2023.1"

intellij {
  version.set(ideaVersion)
  type.set("IU")
  plugins.set(listOf("java", "org.jetbrains.kotlin"))
  updateSinceUntilBuild.set(false)
}

dependencies {
  implementation("ch.qos.logback:logback-classic:1.5.13")
}

tasks {
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  patchPluginXml {
    sinceBuild.set("231.0")
    untilBuild.set("241.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}
