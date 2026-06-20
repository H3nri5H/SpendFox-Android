# SpendFox Android

SpendFox Android is a native Android prototype for personal expense tracking, products and vehicles.

## Prototype scope

The first version is intentionally small and local-first:

- Kotlin
- Jetpack Compose UI
- Material 3 components
- local Room persistence with Supabase-ready sync metadata
- main tabs: overview, contracts, analytics and settings
- feature menu areas: expenses, products and vehicles
- add, search and delete flows
- device unlock through Android credential/biometric prompt after login
- Supabase Auth and PostgREST sync path when configured

## Design direction

SpendFox should stay a native Android app while taking cues from Apple's Human Interface Guidelines:

- content stays clear, readable and direct
- navigation and primary actions may use a light Liquid Glass-inspired treatment
- glass surfaces should not stack on top of other glass surfaces
- controls should feel calm, rounded and easy to tap
- future personal features should appear as new areas instead of forcing every workflow into expense tracking
- accessibility and contrast take priority over decorative transparency

## Requirements

- Windows, macOS or Linux
- Android Studio
- JDK 17, normally bundled with Android Studio
- Android SDK 35 or newer installed through Android Studio

Open the repository in Android Studio and run the **app** configuration on an emulator or Android phone.

## Local configuration

SpendFox can run in local-only prototype mode. To enable the prepared Supabase path, add these keys to `local.properties`:

```properties
supabase.url=https://your-project.supabase.co
supabase.publishableKey=sb_publishable_your-client-key
```

Legacy projects can still use:

```properties
supabase.anonKey=your-public-anon-key
```

Do not commit Supabase secrets. Use only the publishable/anon client key in the app, never a secret or service-role key. Run `supabase/spendfox_schema.sql` in the Supabase SQL editor before using real sync; it creates the app tables and Row Level Security policies.

### Supabase email verification

SpendFox expects signup verification through the 6-digit Supabase email OTP, not through a confirmation link. In Supabase, keep email confirmations enabled and update the **Confirm signup** email template so the user can see the token, for example:

```text
Dein SpendFox Code: {{ .Token }}
```

The app verifies this code with Supabase Auth after registration. The confirmation link can stay in the template as a fallback, but it is no longer required for the in-app registration flow.

## Architecture

```text
app/src/main/java/de/h3nri5h/spendfox/
├── data/      Local models and SQLite store
├── domain/    Money parsing and formatting helpers
├── ui/        Compose screens and ViewModel
└── MainActivity.kt
```

The prototype stores all data locally in `spendfox.db` through Room. Account-owned rows include sync metadata (`userId`, timestamps, deleted marker and sync state) and are pushed to Supabase via PostgREST when a Supabase session is available.

Vehicles now use a tank journal for odometer, fuel amount, fuel cost, consumption and maintenance context. The import/export format is a SpendFox CSV shaped after the existing Excel workbook, but owned by the app so it can evolve cleanly.

## Next milestones

1. Pull remote Supabase rows back into Room with last-write-wins reconciliation.
2. Add a polished XLSX vehicle export beside CSV.
3. Expand contracts into a full recurring-cost area.
4. Add notification permission flow for maintenance reminders.
5. Add UI tests for device unlock, import/export and Supabase-configured login.
