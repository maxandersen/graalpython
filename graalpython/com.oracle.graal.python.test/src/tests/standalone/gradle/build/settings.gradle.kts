rootProject.name = "graalpy-gradle-test-project"

buildscript {
    repositories {
        maven(url="https://repo.gradle.org/gradle/libs-releases/")
    }
    dependencies {
        classpath("org.graalvm.python:graalpy-gradle-plugin:24.2.0")
    }
}