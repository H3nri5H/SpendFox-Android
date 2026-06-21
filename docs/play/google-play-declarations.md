# Google Play Declarations

This file records the current evidence for the Play Console declaration screen.

## Developer Program Policies

Project status: prepared, but not legally self-certifying from source code alone.

Evidence in the app:

- App label is `Nutzblick`.
- Only declared Android permission is `android.permission.INTERNET`.
- No ads SDK, advertising ID usage, location, camera, microphone, contacts, calendar, SMS, call log, package-install, or broad storage permission was found in `app/src/main`.
- Cleartext traffic is disabled in `AndroidManifest.xml`.
- Android backup is disabled in `AndroidManifest.xml`.
- Login and account data access require Supabase Auth.
- Local data is used as an authenticated cache and is cleared on logout.
- Supabase RLS policies restrict every account-owned table to `user_id = auth.uid()`.
- Privacy policy draft: `docs/play/privacy-policy.md`.
- Data safety draft: `docs/play/data-safety.md`.

Manual Play Console tasks:

- Complete App content sections: Privacy policy, Data safety, Content rating, Target audience, Ads declaration, Financial features if Google asks based on category.
- Run one release build and smoke test on a real device before submitting review.
- Do not claim final policy compliance until the store listing and all Play Console forms match the shipped app behavior.

## Play App Signing

Project status: source-ready, Console action required.

What is handled in source:

- Release builds are minified and resources are shrunk.
- App ID for new test publishing is `de.h3nri5h.nutzblick`.
- No upload keystore or signing secret is committed to the repository.

Manual Play Console tasks:

- Enroll this app in Play App Signing.
- Generate or select an upload key locally.
- Store the upload keystore outside git.
- Configure the signing credentials in Android Studio or local Gradle properties only.
- Upload an Android App Bundle built from the release variant.

## US Export Regulations

Project status: prepared, Console declaration required.

The app uses standard platform/network encryption:

- HTTPS/TLS through `HttpURLConnection` for Supabase Auth and PostgREST.
- AndroidX `EncryptedSharedPreferences` for local auth/session storage.
- Android biometric/device credential APIs for local unlock.

The app does not implement custom cryptography, does not expose cryptographic APIs to end users, and does not provide encryption as its primary purpose. This should be declared accurately in Play Console by the account owner. If Google or legal counsel asks for export classification, treat this file as a technical summary, not legal advice.
