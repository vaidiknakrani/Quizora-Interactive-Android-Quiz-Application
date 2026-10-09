# full - Quiz App (single Android app, Java, no Firebase, no backend)

Open the `full` folder in Android Studio (Giraffe/Hedgehog or newer, JDK 17), AGP 8.1.4, Gradle 8.0 and run.

## Gemini key (AI Power Test only)
Option A - development: add to `local.properties` (never commit it):
    GEMINI_API_KEY=your_key_here
    GEMINI_MODEL=gemini-2.5-flash
Option B - on device: Settings > Gemini API key (stored encrypted).
Do not ship a key inside a public APK; for production, proxy Gemini through your own server.

## Privacy policy link
Edit `privacy_policy_url` in `app/src/main/res/values/strings.xml`.

## Unit tests
./gradlew testDebugUnitTest

## Class features
Class Leaderboard, Activity & Streaks (heatmap) and Class Analytics are in the drawer. See `CLASS_FEATURES.md`
for rules, the database migration (v1 -> v2), limits of a local-only app and what a backend would need.

## Naming
Gradle project name is `full`. Activity classes end in `2` (for example `MainActivity2`) and their layouts too
(`activity_main2.xml`, binding `ActivityMain2Binding`). The Java package stays `com.example.adminquiz`.
