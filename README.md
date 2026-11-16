# Personal Utility — Android

A native Kotlin/Jetpack Compose Android project. The initial foundation includes a monochromatic interface, light/dark appearance setting, and a Storage Access Framework-based file browsing foundation.

## Requirements
- JDK 17
- Android SDK Platform 35
- Gradle 8.9 (wrapper configuration included)
- Android Gradle Plugin 8.7.3

## Build
From a configured Android/Gradle environment:

```bash
gradle :app:assembleDebug
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions builds the debug APK on pushes to `main`, pull requests, and manual workflow dispatch. Download it from the workflow run's Artifacts section.

## Storage access
Open the app, tap **Choose folder**, select a folder in Android's system folder picker, then grant access. Access is limited to the selected folder tree and what Android permits. The app does not access other apps' private data or protected system directories.

## Current implementation status
- Implemented in source: app shell, light/dark theme toggle, bottom navigation, persisted selected folder URI, folder listing, folder navigation, refresh, folder creation.
- Not yet implemented: rename/copy/cut/paste/delete, multi-select, advanced search/sort, previews, archives, browser, PWAs, utilities, trash, release signing.
- Build status: not yet verified by a successful GitHub Actions run.
