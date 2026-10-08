plugins {
    id("java-library")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    compileOnly("org.xerial:sqlite-jdbc:3.47.1.0")
    compileOnly("com.mysql:mysql-connector-j:9.7.0")
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.5.9")
    compileOnly("com.zaxxer:HikariCP:7.0.2")
    compileOnly("me.clip:placeholderapi:2.12.3")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}