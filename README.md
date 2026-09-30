# antisocialcord

A pure Kotlin library for communicating with the Android version of Discord, reverse-engineered from
the official SDK from Discord (known as the "Social SDK", hence the joke name)

## Features

* Allows sending an activity update (setting a rich presence) with *zero configuration* on the
  user-side! The user only needs the Discord app installed.

## Usage

Use [Jitpack](https://jitpack.io/) for using this library.
Note that there isn't an actual release of this library just yet, as I'm still figuring out how to
better (re)implement stuff, expect random breakages if you're
not pinning the version to a specific commit hash.

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

```kotlin
dependencies {
    implementation("com.github.Nightdavisao:antisocialcord:master-SNAPSHOT") // just an example, please use a short commit hash instead (for now)
}
```

## Documentation (and examples)

* Dokka-generated docs: https://nightdavisao.github.io/antisocialcord-kt/
* Refer to the `app` module for a simple demo app.

## Projects using antisocialcord

soon tm
