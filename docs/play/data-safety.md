# Data Safety Draft

Use this as the working draft for the Google Play Data safety form. Verify against the final shipped build before submission.

## Data Collected

Account and authentication:

- Email address
- Supabase user ID
- Authentication tokens, stored locally in encrypted preferences

User-provided app data:

- Expenses: amount, merchant, date, category, note, payment account, booking text, purpose, tags
- Products: name, manufacturer, model, serial reference, purchase price, purchase date, warranty date, note
- Vehicles: display name, manufacturer, model, license plate, fuel type
- Trips and fuel journal entries: odometer values, dates, liters, fuel cost, fuel station, notes
- Maintenance items and custom categories
- Optional personal profile values entered in settings: salutation, first name, last name, birth date, address, mobile number

Files:

- Imported banking CSV files are read locally for the import flow.
- Exported CSV/XLSX files are created only after user action through Android document picker APIs.

## Data Shared

Data is sent to the configured Supabase project for authentication and account data storage. No third-party ads, analytics, or advertising ID usage is present in the source tree.

## Purpose

- App functionality
- Account management
- Sync and backup through the user's Supabase-backed account
- Security and fraud prevention through authenticated access control

## Security Practices

- Data in transit uses HTTPS/TLS.
- Supabase Row Level Security restricts database rows to the authenticated owner.
- Auth/session data is stored in Android encrypted preferences.
- App backup is disabled.
- Cleartext traffic is disabled.

## User Controls

- Users can log out, which clears local cached account data and signs out from Supabase.
- Users can delete local data in app flows.
- Final server-side account deletion still needs a confirmed Supabase deletion function before public release.

## Play Form Notes

Mark data as collected when it is sent to Supabase. Do not mark ads or analytics data unless a future SDK adds that behavior.
