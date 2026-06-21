# Nutzblick Privacy Policy Draft

Effective date: to be set before publication.

Nutzblick helps users manage personal expenses, products, vehicles, fuel entries, maintenance context and contract-related notes. Finn Fuchs is an in-app character and does not represent a separate service provider.

## Data We Process

Nutzblick processes data users enter into the app, including account email, expense details, product details, vehicle details, trip and fuel journal entries, maintenance items, custom categories and optional personal profile fields.

## Authentication And Storage

Nutzblick uses Supabase for authentication and account data storage. App data is stored in the configured Supabase project and synchronized to a local on-device cache after sign-in. The local cache is used to display authenticated account data in the app.

Authentication tokens are stored on the device using Android encrypted preferences. Nutzblick disables Android app backup and cleartext network traffic.

## Data Sharing

Nutzblick sends account and app data to the configured Supabase backend so the app can provide login, sync and account recovery. The app source does not include ads SDKs, analytics SDKs or advertising ID usage.

## Imports And Exports

When users import a banking CSV or fuel journal file, Nutzblick reads the selected file locally for the requested import flow. When users export data, Nutzblick writes the selected export file only after the user chooses a destination through Android document APIs.

## Security

Supabase Row Level Security policies restrict account-owned rows to the authenticated user. Data in transit uses HTTPS/TLS. Local auth/session values use Android encrypted preferences.

## User Choices

Users can log out to clear local cached account data and end the Supabase session. Account deletion must be completed through a confirmed server-side Supabase deletion flow before public release.

## Contact

Set the support email and legal contact address before publication.

This draft must be reviewed and published at a stable public URL before submitting Nutzblick to Google Play.
