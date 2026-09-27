plugins {
    id("java")
}

group = "com.rdcsq"
version = "1.0.0"

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://repo.onarandombox.com/content/groups/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    compileOnly("org.mvplugins.multiverse.core:multiverse-core:5.8.1")
}