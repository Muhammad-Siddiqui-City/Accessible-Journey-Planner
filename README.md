# Accessible Journey Planner

Android app for accessible travel planning in London, built with Java/XML using MVVM, Retrofit + Gson, Room, and WorkManager.

## Repository

- GitHub: https://github.com/Muhammad-Siddiqui-City/Accessible-Journey-Planner

## Tech stack

- Language/UI: Java, Android XML layouts
- Architecture: MVVM (ViewModel + LiveData)
- Networking: Retrofit + Gson (TfL), OkHttp (National Rail OpenLDBWS)
- Local storage: Room
- Background tasks: WorkManager
- Min Android API: 26

## Project structure

```text
<project root>/
├── app/
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/ajp/
│       │   │   ├── api/
│       │   │   ├── data/local/
│       │   │   ├── ui/
│       │   │   ├── utils/
│       │   │   └── widgets/
│       │   └── res/
│       │       ├── layout/
│       │       ├── drawable/
│       │       ├── values/ (and values-*)
│       │       └── menu/
│       └── test/java/
├── mylibrary/
│   ├── build.gradle
│   └── src/
│       ├── main/AndroidManifest.xml
│       ├── test/java/
│       └── androidTest/java/
├── gradle/wrapper/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.properties.example
├── JAVA_NON_AI_LINES.txt
└── README_Submission.md
```

## Build and run

1. Open this folder in Android Studio Hedgehog (2023.1.1) or later.
2. Copy `local.properties.example` to `local.properties`.
3. Set keys in `local.properties`:
   - `TFL_APP_KEY`
   - `RAIL_ACCESS_TOKEN`
4. Sync Gradle.
5. Run the `app` configuration on an emulator/device (API 26+).

## Testing

- Unit tests: `app/src/test/java`
- `mylibrary` template tests: `mylibrary/src/test/java` and `mylibrary/src/androidTest/java`

## Notes

- Keep secrets local only (`local.properties` is gitignored).
- `JAVA_NON_AI_LINES.txt` is a submission artefact containing concatenated Java source lines.
