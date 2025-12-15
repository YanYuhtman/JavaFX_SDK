# JavaFX SDK

A simple, opinionated framework for building modern, context-aware JavaFX applications in Kotlin. It provides a clear structure, dependency injection, and useful utilities to accelerate development.

## Features

-   **MVVM-like Architecture:** A clear separation of concerns with `AbstractApplication`, `AbstractScene`, `AbstractController`, and `AbstractModel`.
-   **Context-Aware Components:** Easily access application-wide services and state through a shared context.
-   **Structured Concurrency:** Built-in support for Kotlin Coroutines with lifecycle-aware scopes (`appScope`, `sceneScope`, `modelScope`).
-   **Localization Support:** A robust localization system (`Localization`) that loads translations from resource bundles.
-   **Compile-Time Message Keys:** Includes a KSP (Kotlin Symbol Processing) processor that generates a `Messages` object from your `.properties` files, providing compile-time safety for localization keys.
-   **Weak-Referenced Event System:** A flexible `EventHandler` system that helps prevent memory leaks.
-   **Performance Utilities:** Includes helpers like `BufferedUpdater` to prevent UI freezes from high-frequency updates.

## Project Modules

The project is structured into three main modules:

1.  **`javafx-sdk`**: The core framework containing all the abstract classes, utilities, and UI helpers. Your application will primarily depend on this.
2.  **`annotations`**: Contains annotations used by the KSP processor. (Currently used implicitly).
3.  **`ksp`**: The KSP processor that automatically generates message key constants.

## Getting Started

To use this SDK in your Gradle project, you need to set up your `build.gradle.kts` to include the SDK modules and configure the KSP processor.

### 1. Project Setup

Assuming you have published the artifacts to your local Maven repository (`./gradlew publishToMavenLocal`), you can add them as dependencies.

Here is a sample `build.gradle.kts` configuration for a new JavaFX application using this SDK:

```kotlin
plugins {
    java
    application
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("com.google.devtools.ksp") version "2.1.20-1.0.31"
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal() // For accessing the locally published SDK
}

// Java and Kotlin compiler settings
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
kotlin {
    jvmToolchain(17)
}

// JavaFX configuration
javafx {
    version = "21" // Use a relevant JavaFX version
    modules = listOf("javafx.controls", "javafx.fxml")
}

// Application settings
application {
    mainModule.set("com.example.myapp") // Your app's module name
    mainClass.set("com.example.myapp.MainKt") // Your app's main entry point
}

// KSP configuration for message key generation
ksp {
    // All arguments are optional. Defaults will be used if not provided.

    // The package where the 'Messages' object will be generated.
    // Default: "com.ileveli.ksp"
    arg("genPackage", "com.example.myapp.gen")

    // A specific path to the resources directory. If not set, the processor
    // will try to find it automatically.
    // Default: ""
    arg("resourceDirPath", "/path/to/your/resources")

    // The default character set for reading properties files.
    // Default: "UTF-8"
    arg("charSet", "UTF-8")

    // Charset for certain language resource 
    arg("charSet_ru", "Windows-1251")

    // Enable debug logging for the processor.
    // Default: "false"
    arg("debug", "true")
}

dependencies {
    // SDK modules
    implementation("com.ileveli:javafx-sdk:1.0.2") // Use the correct version
    ksp("com.ileveli:ksp:1.0.2") // KSP processor
    implementation("com.ileveli:annotations:1.0.0") // Annotations (if needed directly)

    // Standard Kotlin libraries
    implementation(kotlin("stdlib"))

    // Required by the SDK
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("ch.qos.logback:logback-classic:1.5.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    // Testing
    testImplementation(kotlin("test"))
}
```

### 2. Create Resource Files

Create your localization files in `src/main/resources`. The KSP processor will find these and generate constants.

**`src/main/resources/Messages_en.properties`**
```properties
appTitle=My App
buttonClickMe=Click Me!
labelWelcome=Welcome
```

**`src/main/resources/Messages_ru.properties`**
```properties
appTitle=Моё Приложение
buttonClickMe=Нажми Меня!
labelWelcome=Добро пожаловать
```

### 3. Build the Project

Run a Gradle build. The KSP processor will automatically generate the `Messages.kt` file.

```bash
./gradlew build
```

**Generated File (`build/generated/ksp/main/kotlin/com/example/myapp/gen/Messages.kt`)**
```kotlin
package com.example.myapp.gen

object Messages {
    const val appTitle:String = "appTitle"
    const val buttonClickMe:String = "buttonClickMe"
    const val labelWelcome:String = "labelWelcome"
}
```
*Note: The generated property names will be sanitized to be valid Kotlin identifiers if they contain invalid characters.*

## Core Concepts

The framework is built around a few key abstract classes that you extend to create your application.

### `AbstractApplication`
This is the main entry point of your app. You extend this class to set up global context and define how your main scene is created.

```kotlin
class MyApp : AbstractApplication() {
    // Used for app data directory and resource paths
    override val packageName = "com.example.myapp"

    override fun start(primaryStage: Stage) {
        // Set the initial locale if needed
        setLocale(Locale("en"), resetUI = false)

        super.start(primaryStage)
        primaryStage.title = getString(Messages.appTitle) // Using generated key
        primaryStage.show()
    }

    // This is called by start() and restartUI() to create the main scene
    override fun mainSceneResolver(stage: Stage): Scene {
        return MainScene(this)
    }
}

fun main() {
    Application.launch(MyApp::class.java)
}
```

### `AbstractScene` & `AbstractFXMLScene`
A scene represents a single screen or view. `AbstractFXMLScene` is a specialized version for loading views from FXML files. It automatically handles FXML loading, controller initialization, and provides a coroutine scope (`sceneScope`) that is cancelled when the scene is detached.

```kotlin
// MainScene.kt
class MainScene(appContext: MyApp) : AbstractFXMLScene<MyApp, MainController>(
    appContext = appContext,
    fxmlResourcePath = "main-view.fxml"
)
```

### `AbstractController`
The controller manages the logic for a view. It's automatically initialized with the application context and provides access to the coroutine scopes and UI elements defined in your FXML.

```kotlin
// MainController.kt
class MainController : AbstractController<MyApp>() {
    @FXML
    private lateinit var welcomeLabel: Label

    @FXML
    private lateinit var actionButton: Button

    override fun onContextInitialized(appContext: MyApp) {
        // Initialization logic here, context is ready to use.
        welcomeLabel.text = appContext.getString(Messages.labelWelcome)
        actionButton.text = appContext.getString(Messages.buttonClickMe)
    }

    @FXML
    private fun onButtonClick() {
        // Toggle locale on button click
        if (appContext.locale.language == "en") {
            appContext.setLocale(Locale("ru"))
        } else {
            appContext.setLocale(Locale("en"))
        }
    }
}
```

### `AbstractModel`
Models hold state and business logic. They are attached to scenes and can be shared. The framework ensures they are properly attached and detached, managing their lifecycle and coroutine scopes.

```kotlin
// CounterModel.kt
class CounterModel : AbstractControllerModel<MyApp, MainScene, MainController>() {
    val count = SimpleIntegerProperty(0)

    override fun OnAttached() {
        // Called when the model is fully attached to the scene and controller
        Logger.info { "CounterModel attached!" }
    }

    fun increment() {
        count.set(count.get() + 1)
    }

    override fun OnSceneShown() {}
    override fun OnDetached() {}
}
```

To use the model, attach it in your scene:
```kotlin
// MainScene.kt
class MainScene(appContext: MyApp) : AbstractFXMLScene<MyApp, MainController>(...) {
    init {
        // Attach a model to this scene's lifecycle
        attachModel(CounterModel())
    }
}
```

## Key Advantages

Beyond the basic structure, the framework provides powerful advantages for building robust applications.

### Lifecycle Awareness

The SDK automatically manages the lifecycle of your scenes and models, helping to prevent common issues like memory leaks.

-   **Automatic Cleanup:** When a window is closed, the associated `AbstractScene` is automatically "detached".
-   **Cascading Cancellation:** Detaching a scene cancels its `sceneScope`, which in turn triggers the detachment of all associated `AbstractModel` instances and the cancellation of their `modelScope`s.
-   **Simplified Resource Management:** You can hook into the `OnDetached()` method in your models to perform any custom cleanup, confident that it will be called at the right time.

This means you don't have to manually track scene or model lifecycles to release resources or stop background tasks.

### Structured Concurrency & Ready-to-use Scopes

The framework fully embraces Kotlin's structured concurrency, providing lifecycle-aware coroutine scopes out of the box. This makes asynchronous programming in JavaFX safe and simple.

-   **`appScope` (`AbstractApplication`):** A top-level scope that lives for the entire duration of the application. Ideal for long-running background services or tasks that are not tied to a specific UI screen.

-   **`sceneScope` (`AbstractScene`):** Each scene gets its own scope, which is a child of the `appScope`. It's created when the scene is initialized and automatically cancelled when the scene is detached. This is the perfect place for tasks that are only relevant while a specific scene is visible.

    ```kotlin
    // In a subclass of AbstractScene
    fun loadData() {
        sceneScope.launch {
            val data = myApiService.fetchData() // Background task
            withContext(Dispatchers.Main) {
                updateUI(data) // Update UI on the JavaFX thread
            }
        }
    }
    ```

-   **`modelScope` (`AbstractModel`):** Each model gets its own scope, which is a child of its scene's `sceneScope`. It's created when the model is attached and cancelled when detached. This is the go-to scope for running business logic or data operations within your models.

    ```kotlin
    // In a subclass of AbstractModel
    fun saveData(data: UserData) {
        modelScope.launch(Dispatchers.IO) {
            // This coroutine is automatically cancelled if the model is detached
            database.save(data)
        }
    }
    ```

This clear hierarchy of scopes ensures that background tasks are automatically cleaned up when the UI component they belong to is destroyed, preventing resource leaks and unexpected behavior.

## Building The Project

To build the entire SDK from the source, run:

```bash
./gradlew build
```

To publish the artifacts to your local Maven repository for testing in other projects:

```bash
./gradlew publishToMavenLocal
```

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
