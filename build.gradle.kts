plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.serceman:jnr-fuse:0.5.7")
    implementation("ch.qos.logback:logback-classic:1.5.19")
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.shadowJar {
    archiveFileName.set("fuse.jar")
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "fuse.SpyFs"
    }
}
