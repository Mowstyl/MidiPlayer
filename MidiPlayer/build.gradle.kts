plugins {
  `java-library`
  id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
}

group = "org.primesoft.midiplayer"
version = "0.4.0"
description = "A plugin that allows you to play custom music on your server"

java {
  // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 11 installed for example.
  toolchain.languageVersion = JavaLanguageVersion.of(17)
}

dependencies {
  paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
  compileOnly("com.googlecode.json-simple:json-simple:1.1.1")
}

tasks {
  compileJava {
    // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
    // See https://openjdk.java.net/jeps/247 for more information.
    options.release = 17
  }

  processResources {
    filteringCharset = Charsets.UTF_8.name()
    val props =
      mapOf(
        "name" to project.name,
        "version" to project.version,
        "description" to project.description,
      )
    inputs.properties(props)
    filesMatching("paper-plugin.yml") { expand(props) }
  }
}
