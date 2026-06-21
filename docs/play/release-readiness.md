# Release Readiness Checklist

## Must Be Done Before Google Play Review

- Run `supabase/nutzblick_schema.sql` in the target Supabase project.
- Verify Supabase Auth email OTP template contains the code token.
- Configure `supabase.url` and `supabase.publishableKey` in local release build properties.
- Build the release Android App Bundle with an upload key stored outside git.
- Enroll in Play App Signing in Google Play Console.
- Publish the privacy policy draft at a stable public URL and add that URL to Play Console.
- Complete Play Console Data safety using `docs/play/data-safety.md`.
- Complete Play Console Content rating and Target audience sections.
- Complete the US export declaration using `docs/play/google-play-declarations.md` as the technical basis.
- Smoke test registration, OTP verification, login, device unlock, Supabase pull, create/edit/delete and logout on a real Android device.

## Current Source-Level Controls

- Only `INTERNET` permission is declared.
- App backup is disabled.
- Cleartext traffic is disabled.
- Release minification and resource shrinking are enabled.
- Supabase is required for login and account data access.
- Local Room database is an authenticated cache, not the source of truth.
- Supabase RLS is enabled and forced for account tables.
- `anon` database table privileges are revoked.
- Auth/session data uses encrypted preferences.

## Known Release Blockers

- Server-side account deletion is still not implemented.
- Refresh-token rotation and expired-session recovery are still listed as a next milestone.
- A JDK must be available locally to run Gradle verification commands.
