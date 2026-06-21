# Nutzblick Android

Nutzblick Android is a native Android app for personal expense tracking, products, vehicles and contract context. Finn Fuchs remains the in-app contact character and guide.

## Test publish scope

The current version is scoped for a Supabase-backed test publish:

- Kotlin
- Jetpack Compose UI
- Material 3 components
- local Room cache fed from Supabase after authentication
- main tabs: overview, contracts, analytics and settings
- feature menu areas: expenses, products and vehicles
- add, search and delete flows
- device unlock through Android credential/biometric prompt after login
- Supabase Auth and PostgREST as the required account data path

## Design direction

Nutzblick should stay a native Android app while taking cues from Apple's Human Interface Guidelines:

- content stays clear, readable and direct
- navigation and primary actions may use a light Liquid Glass-inspired treatment
- glass surfaces should not stack on top of other glass surfaces
- controls should feel calm, rounded and easy to tap
- green and petrol tones carry the product surface, while fox orange is reserved as an accent
- future personal features should appear as new areas instead of forcing every workflow into expense tracking
- accessibility and contrast take priority over decorative transparency

## Requirements

- Windows, macOS or Linux
- Android Studio
- JDK 17, normally bundled with Android Studio
- Android SDK 35 or newer installed through Android Studio

Open the repository in Android Studio and run the **app** configuration on an emulator or Android phone.

## Supabase configuration

Nutzblick requires Supabase before users can register or sign in. Add these keys to `local.properties`:

```properties
supabase.url=https://your-project.supabase.co
supabase.publishableKey=sb_publishable_your-client-key
```

Legacy projects can still use:

```properties
supabase.anonKey=your-public-anon-key
```

Do not commit Supabase secrets. Use only the publishable/anon client key in the app, never a secret or service-role key. Run `supabase/nutzblick_schema.sql` in the Supabase SQL editor before using real data; it creates the app tables and Row Level Security policies.

## Google Play readiness

Play Console declarations are tracked in `docs/play/google-play-declarations.md`. The data-safety draft is in `docs/play/data-safety.md`, the privacy policy draft is in `docs/play/privacy-policy.md`, and the release checklist is in `docs/play/release-readiness.md`.

The Play App Signing terms and the US export declaration must still be accepted by the Play Console account owner in Google Play Console. The project is prepared for those declarations, but they cannot be completed from source code alone.

### Supabase email verification

Nutzblick expects signup verification through the 6-digit Supabase email OTP, not through a confirmation link. In Supabase, keep email confirmations enabled and update the **Confirm signup** email template so the user can see the token, for example:

```text
Dein Nutzblick Code: {{ .Token }}
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

The app uses `nutzblick_cache.db` as a local cache only. After login, account rows are pulled from Supabase and replace the local cache for that user. Writes and soft-deletes are sent to Supabase through PostgREST with the authenticated user token.

Vehicles use a tank journal for odometer, fuel amount, fuel cost, consumption and maintenance context. The import/export format is a Nutzblick CSV shaped after the existing Excel workbook, but owned by the app so it can evolve cleanly.

## Next milestones

1. Add refresh-token rotation and expired-session recovery.
2. Add a polished XLSX vehicle export beside CSV.
3. Expand contracts into a full recurring-cost area.
4. Add notification permission flow for maintenance reminders.
5. Add UI tests for device unlock, import/export and Supabase-configured login.
