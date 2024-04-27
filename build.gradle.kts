plugins {
    kotlin("jvm") version "2.0.0-RC1"

    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("antlr")

    application
}

repositories {
    mavenCentral()
}

dependencies {
    // Use RxKotlin
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")

    // Use Logger
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Use JDT
    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.37.0")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use ANTLR
    antlr("org.antlr:antlr4:4.13.1")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "jp.ac.osaka_u.sdl.nil.NILMainKt"
}

tasks.generateGrammarSource {
    enabled = false
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
