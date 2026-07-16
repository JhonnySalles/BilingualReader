dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
        maven {
            url = java.net.URI("https://maven.google.com/")
            name = "Google"
        }
        gradlePluginPortal()
    }
}
rootProject.name = "BilingualReader"
include(":BilingualReader")
include(":ebook")
