# SpendFox Android

SpendFox Android is a native Android prototype for personal expense tracking, products and vehicles.

## Prototype scope

The first version is intentionally small and local-first:

- Kotlin
- Jetpack Compose UI
- Material 3 components
- local SQLite persistence
- five app areas: overview, expenses, products, vehicles and settings
- add, search and delete flows
- prototype seed data on first launch
- no cloud account, analytics or external API calls

## Requirements

- Windows, macOS or Linux
- Android Studio
- JDK 17, normally bundled with Android Studio
- Android SDK 35 or newer installed through Android Studio

Open the repository in Android Studio and run the **app** configuration on an emulator or Android phone.

## Architecture

```text
app/src/main/java/de/h3nri5h/spendfox/
├── data/      Local models and SQLite store
├── domain/    Money parsing and formatting helpers
├── ui/        Compose screens and ViewModel
└── MainActivity.kt
```

The prototype stores all data locally in `spendfox.db`. The data layer is intentionally isolated so it can later be replaced by Room migrations or a Supabase synchronization adapter.

## Next milestones

1. Add a proper Gradle wrapper if Android Studio does not generate one automatically.
2. Replace simple dialogs with dedicated add/edit screens.
3. Add Room with explicit migrations.
4. Add native ING CSV import.
5. Expand categories and category selection.
6. Add local export and backup.
7. Decide whether Android or iOS becomes the primary implementation path.
