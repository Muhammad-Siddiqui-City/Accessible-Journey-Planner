# Accessible Journey Planner (Android)

Native Android app (Java, XML layouts, minSdk 26) for step-free journey planning, live arrivals, and related features. Uses **MVVM** (ViewModels + LiveData), **Retrofit** and **Gson** for TfL REST APIs, **OkHttp** for National Rail OpenLDBWS (SOAP), **Room** for on-device persistence, and **WorkManager** for periodic route monitoring.

**Source repository:** [github.com/Muhammad-Siddiqui-City/Accessible-Journey-Planner](https://github.com/Muhammad-Siddiqui-City/Accessible-Journey-Planner)

---

## Project directory layout

Open the **repository root** in Android Studio (the folder that contains `settings.gradle`). Overview:

```text
<project root>/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/ajp/     # Application code (api, data.local, ui.*, utils, widgets)
│       │   └── res/                      # Layouts, drawables, values, values-* (locales), menu, color
│       └── test/java/                    # JUnit unit tests
├── mylibrary/                            # Secondary Android library module (included in settings.gradle)
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/AndroidManifest.xml
│       ├── test/java/                    # Example unit test (template)
│       └── androidTest/java/             # Example instrumented test (template)
├── gradle/wrapper/                       # Gradle Wrapper (gradle-wrapper.properties, jar)
├── build.gradle                          # Root Gradle build
├── settings.gradle                       # includes ':app' and ':mylibrary'
├── gradle.properties
├── gradlew / gradlew.bat
├── local.properties.example              # Template for keys (copy to local.properties)
├── JAVA_NON_AI_LINES.txt                 # Concatenated Java sources (submission / line-count artefact)
└── .gitignore
```

Do **not** commit `local.properties` (it contains machine-specific paths and secrets). Generated `build/` folders are ignored by Git.

---

## Build and run

**IDE:** **Android Studio Hedgehog (2023.1.1) or later** (recommended).

**Requirements:** **JDK 17** (see `app/build.gradle` Java toolchain), **Android SDK** with **compileSdk 34**, run on **emulator or device API 26+**.

1. Clone or open the project from the [GitHub repository](https://github.com/Muhammad-Siddiqui-City/Accessible-Journey-Planner).
2. Open the **project root** in Android Studio.
3. Copy **`local.properties.example`** to **`local.properties`** in the project root (if missing) and set the values below.
4. Sync Gradle (**File → Sync Project with Gradle Files**).
5. Run the **`app`** configuration on an emulator or device.

---

## API keys (`local.properties`)

Keys are read only from **`local.properties`** at the project root (see `app/build.gradle`). They are **not** stored in `res/values/secrets.xml` or similar XML resources.

| Property | Purpose |
|----------|---------|
| **`sdk.dir`** | Path to your Android SDK (Android Studio usually sets this automatically). |
| **`TFL_APP_KEY`** | TfL Unified API **app key** (matches TfL’s `app_key` terminology). Register at [api.tfl.gov.uk](https://api.tfl.gov.uk/). **Use this exact name** — do **not** use `tfl_api_key` or other aliases unless you change `build.gradle` and `ApiKeyManager` to match. |
| **`RAIL_ACCESS_TOKEN`** | National Rail **OpenLDBWS** access token (used for national rail departure data where implemented). Set here **only** — not in `secrets.xml`. |

Runtime access: **`BuildConfig.TFL_APP_KEY`** and **`BuildConfig.RAIL_ACCESS_TOKEN`**. Keys must not be committed to Git.

---

## Tests

- **Unit tests:** `app/src/test/java` — JUnit 4 and Mockito (core logic helpers, adapters, etc.).
- **Instrumented/UI tests** are not part of this prototype; see the project report for scope.

---

## Main screens

- **MainActivity** — Bottom navigation: Home, Journeys, Analytics, Settings.
- **Home** — Search, quick links, disruptions, saved routes, nearby stations (opens arrivals / search flows).
- **Journey (Plan Journey)** — Origin/destination, filters, **Find Routes**; TfL journey results; **route preview** uses a **Leaflet + OpenStreetMap WebView** (`wv_route_map`) when coordinates are available. Tap a route → **Route Details**.
- **Live Arrivals** — Nearby stops and arrivals (TfL; national rail flows where applicable).
- **Route Details** — Full-screen: summary, step-by-step legs, bookmark/share, optional **Start Navigation** / live progress; **no** embedded Leaflet map (map preview is on the Journey screen).
- **Analytics** — Journey stats and charts (local data).
- **Settings** — Accessibility, language, mobility preferences, links.
- **Feedback** — Feedback form activity.

---

## Package layout (`app` module)

- `com.example.ajp.api` — REST DTOs, Retrofit API, SOAP client (National Rail).
- `com.example.ajp.data.local` — Room entities, DAOs, database.
- `com.example.ajp.ui.*` — Fragments, activities, adapters, view models.
- `com.example.ajp.utils` — Networking helpers, journey fetch, monitoring, preferences, accessibility helpers.
- `com.example.ajp.widgets` — Custom views (charts, badges, etc.).

---

## Notes

- No user accounts; data is stored **locally** (Room) aside from API calls over HTTPS.
- Route map tiles: OpenStreetMap (see in-app attribution on the WebView map).
