# Modern Android Development Essentials

## 1. Android Architecture (MAD)
The recommended architecture is **Layered Architecture** with a clear separation of concerns.
* **UI Layer:** Composed of UI elements (Jetpack Compose) and **State Holders** (ViewModels). It transforms application state into visual elements.
* **Domain Layer (Optional):** Sits between UI and Data layers. It contains **UseCases** that encapsulate complex business logic or reusable logic across multiple ViewModels.
* **Data Layer:** The **Single Source of Truth (SSOT)**. Consists of **Repositories** that coordinate data from various **Data Sources** (Remote API with Retrofit, Local Database with Room, or DataStore for preferences).

---

## 2. Android Coroutines
A concurrency framework to write asynchronous, non-blocking code sequentially.
* **Scopes:** `viewModelScope` (cancels when ViewModel is cleared) and `lifecycleScope` (tied to Activity/Fragment lifecycle).
* **Dispatchers:** * `Dispatchers.Main`: UI interactions and small tasks.
    * `Dispatchers.IO`: Optimized for disk/network I/O (API calls, Database).
    * `Dispatchers.Default`: Optimized for CPU-intensive work (Sorting, Image processing).
* **Structured Concurrency:** Ensures that when a scope is cancelled, all coroutines started in that scope are also cancelled, preventing memory leaks.

---

## 3. Android Data Layer
Responsible for providing data to the rest of the app and managing business logic.
* **Repositories:** The entry point to the data layer. They decide whether to fetch data from the network or use a local cache.
* **Data Sources:** Encapsulate the implementation of data providers (e.g., `UserRemoteDataSource` for Retrofit, `UserLocalDataSource` for Room).
* **Immutability:** Data models (DTOs or Entities) should be immutable to ensure thread safety across different layers.

---

## 4. Compose Navigation
The declarative way to handle navigation in a Compose-only app.
* **NavController:** The central coordinator that manages the backstack and navigation state.
* **NavHost:** The container that hosts the navigation graph and defines all possible destinations.
* **Type Safety:** Since Navigation 2.8.0+, routes can be defined using Kotlin **Serializable** objects/classes instead of raw Strings, ensuring compile-time safety.
* **Navigation Compose:** Seamlessly integrates with the UI, allowing you to pass arguments and handle deep links easily.

---

## 5. Compose Performance Audit
Strategies to ensure high-performance UI and avoid "jank."
* **Recomposition:** Minimize the number of composables that need to restart. Use `remember` and `derivedStateOf` to cache expensive calculations.
* **Stability:** Use `@Stable` or `@Immutable` annotations for custom classes to help the compiler skip unnecessary recompositions.
* **Phases (Composition -> Layout -> Drawing):** Defer state reading to the latest possible phase (e.g., using a lambda in `Modifier.offset { ... }` instead of `Modifier.offset(...)`).
* **Baseline Profiles:** Provide a list of critical code paths to the Android system to pre-compile them, significantly improving app startup and scroll performance.

---

## 6. Compose UI
A modern, declarative toolkit for building native Android UIs.
* **Declarative Paradigm:** You describe the UI's end state based on the current data; Compose automatically updates the UI when the state changes.
* **State Hoisting:** The pattern of moving state to a caller to make a composable stateless, making it more reusable and easier to test.
* **Modifiers:** Ordered decorations that modify a composable’s behavior or appearance (padding, clicks, size, graphics).
* **CompositionLocal:** A mechanism to pass data (like themes, colors, or contexts) through the UI tree implicitly without passing them as parameters to every single composable.