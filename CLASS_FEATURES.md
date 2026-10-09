# Quizora - class features (Whole-Class Leaderboard, Daily Streaks, Class Analytics)

Everything below lives inside the single `full` app. Nothing was removed or renamed; Ghost Match, Creator
Quizzes, Persona Results, AI Power Test, quiz engine, share codes, login, profile and the old global
Leaderboard behave as before. Three new drawer items were added after "Leaderboard": **Class Leaderboard**,
**Activity & Streaks**, **Class Analytics**. The Home screen is untouched.

## 1. What a "class" is (and the honest limit)
The app has no server, so a class is a **class code** stored on each student's own account
(`users.classCode`, default `GENERAL`). Students entering the same code at registration (or later in
Settings > My Class) are ranked together.
**Rankings include only students who have accounts on the same device/database.** Cross-device rankings are
not possible without a backend. What that backend must provide before it can be built:
1. Authenticated accounts (token per student) and class membership stored server-side.
2. An endpoint to submit a finished attempt (answers + timing + a client attempt UUID for idempotency). The
   server recomputes score, validity and the timestamp itself; clients never send points or streak days.
3. Read-only endpoints for leaderboard / analytics aggregates scoped to the caller's class; no write access to
   other students' rows.
4. An outbox in Room (pending attempts) that syncs when online and de-duplicates by attempt UUID.
Because that is a separate product decision, no half-built sync code was added.

## 2. Database change (Room v1 -> v2)
* One new column: `users.classCode TEXT`.
* `MIGRATION_1_2`: `ALTER TABLE users ADD COLUMN classCode TEXT` then `UPDATE users SET classCode='GENERAL'`.
  Existing users, quizzes, attempts and answers are preserved. `fallbackToDestructiveMigration()` remains only
  as a safety net for unknown older versions.
* No new tables: leaderboard, streaks and analytics are computed from the existing `attempts`, `answers`,
  `quizzes`, `users` tables, so they cannot drift out of sync with the real results.

## 3. Rules
**Valid attempt:** `total > 0` and at least half of the questions answered. Duplicates (same attempt id),
attempts with a time more than 5 minutes in the future, and zero-question attempts are ignored.

**Leaderboard:** per student and quiz only the *best* attempt in the period counts (replaying cannot add
points). Points = sum of those best scores. Accuracy = correct / questions over the same attempts.
Points order: points, accuracy, fewer quizzes, less total time. Accuracy order: accuracy, more quizzes,
points, less total time, and needs at least 3 quizzes in the period (1 for Today) so one lucky quiz cannot win.
Students equal on every key share a rank. Periods use local calendar days: Today, last 7 days, last 30 days,
all time. The student's own row is highlighted and a pinned "Your rank" card shows their rank even outside the
top 25 (or explains why they are not ranked yet).

**Streaks:** an active day = at least one valid quiz on that *local* calendar date. Many quizzes on one day =
one active day. Current streak = consecutive active days ending today, or yesterday if today has no quiz yet.
Longest streak is the longest run ever and is never reduced. Heat level by quizzes per day: 0, 1, 2, 3-4, 5+.
Badges at 3, 7, 14, 30, 100, 365 days. Day maths uses calendar dates, so DST, leap days, month and year
boundaries are handled (covered by unit tests).

**Anti-tamper (on-device limits):** finish times come from `TrustedClock`, which compares wall time with the
monotonic clock and ignores a clock jump larger than 10 minutes inside one boot; future-dated attempts are
ignored; repeated submissions cannot add streak days or points. A person who changes the clock *forward*, finishes a
quiz and reboots can still record that time. Truly tamper-proof history needs server time (section 1).

**Analytics:** enrolled = users in the class; participants = unique students with a valid attempt in the
selection; completed = valid attempts; participation = participants / enrolled. Filters: 7/30/90 days or
all time, subject, quiz. Charts: accuracy over time (class vs you), participation per day/week, score
distribution, accuracy by subject and by quiz, your last 20 attempts vs class average. Strongest / weakest
subject only appear when you have at least two subjects. Average time uses the recorded quiz timings.

## 4. Files
New: `analytics/` (StreakCalculator, LeaderboardRanker, ClassAnalytics, AttemptRecord, ClockGuard),
`utils/ClassCode`, `utils/TrustedClock`, `repositories/ClassRepository`, four ViewModels
(ClassLeaderboard, Streak, ClassAnalytics, ClassSettings), `views/` (HeatmapView, BarChartView, LineChartView,
GroupedBarsView, BaseChartView), `fragments/` (ClassLeaderboard, ActivityStreak, ClassAnalytics),
`adapters/ClassRankAdapter`, layouts `fragment_class_leaderboard`, `item_class_rank`,
`fragment_activity_streak`, `fragment_class_analytics`, drawables `bg_avatar_circle`, `bg_badge_pill`,
`bg_badge_locked`, tests `ClassAnalyticsTest`, `LeaderboardRankerTest`, `StreakCalculatorTest`,
`ClockGuardTest`, `TestData`.
Modified (small, targeted): `UserEntity` (+classCode), `AppDatabase` (v2 + migration), `UserDao`, `AttemptDao`
(+queries), `AttemptRepository` (trusted time), `AuthRepository` / `AuthViewModel` / `RegisterActivity2` /
`activity_register2.xml` (optional class code), `StatsRepository` (profile streak now uses the calendar rules),
`MainActivity2` (3 top-level ids), `drawer_menu.xml`, `nav_graph.xml`, `strings.xml`, `SettingsFragment` +
`fragment_settings.xml` (My Class section; layout is now scrollable).

## 5. Security
Every query reads only the signed-in student's class; the only write is changing *your own* class code
(`ClassRepository.changeClass(userId, ...)`). There is no UI or API to edit scores, attempts or activity.

## 6. Verification performed here (honest status)
* Pure-Java logic (analytics + ClassCode + ClockGuard): **37 JUnit tests pass** (javac + JUnit 4.13.2).
* All XML parses; every `@string`, `R.string`, `R.id`, `@drawable` reference and every ViewBinding field used by
  the new/changed screens exists; all Java sources pass a syntax parse.
* **Not done:** a real Gradle/Android build, Room annotation processing, running on an emulator or device, and
  `QuizEngineTest` (needs Android). The sandbox cannot reach Google Maven / Gradle servers. Please run
  `./gradlew testDebugUnitTest assembleDebug` in Android Studio and report any compile message so it can be fixed.

Manual test checklist: register two students with the same class code and play quizzes; check Class Leaderboard
filters and the pinned "Your rank"; play twice in one day (streak stays +1); change device date to yesterday's
data in tests only; open Activity & Streaks and tap heatmap cells; open Class Analytics and change filters;
turn on airplane mode (everything still works); toggle dark mode.
