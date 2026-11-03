# Ruvo — project guide for Claude Code

Ruvo is a final-year UI/UX portfolio project by Kumkum: a native Android student money app for Indian
students aged 18–25 living away from home. It is not going on the Play Store. The deliverables are an
installable APK, a 60–90 s screen recording, and a new case-study deck. The full locked plan is in
`docs/PLAN.md`; this file is the short version plus how to work in this repo.

## The one idea

Everything is organised around one number: **Runway**, in days.

- Big "23 days" labelled Runway, with "Next money in 19 days · 4 days spare" beneath.
- Buffer days = runway days − days until next income. +4 means you make it with 4 days spare.
- Three buckets: **Fixed** (already promised), **Fuel** (free to spend), **Future** (protected for goals).
- The maths lives in `core/engine` and nowhere else. The AI only explains engine results; it never computes.

## Locked decisions (do not change without asking Kumkum)

| Area | Decision |
| --- | --- |
| Platform | Android only, Kotlin + Jetpack Compose, min SDK 26, no backend, no login |
| Navigation | Three tabs (Today, Money, Goals) on a custom floating dock; not a stock Material bar |
| Companion | The **Ruvo Orb**: a liquid sphere, no face. Fill = Fuel; ripples faster when money is tight; glows when a goal completes. Poke, drag, tilt. Tap = talk to Ruvo, pull down = quick-add. Replaces mascot, AI button and capture button |
| Visual | Two moods with a switch: light "Daybreak" and dark "Night swim". Brand palette used freely. Custom icons, no emoji. Indian number grouping (₹1,25,000), dates like "3 Nov". Identical bucket cards, each with name and icon so colour is never the only cue |
| Typeface | Anek Latin (variable width and weight, includes ₹). Display uses the expanded width. Downloaded at build time by `core/design` |
| Voice | Written persona: mature, warm, insightful Indian woman's voice in English. Optional spoken replies via the phone's Indian English TTS |
| AI | Scripted offline answers by default so the demo never fails. Optional live Claude API with the user's own key, sending only a summary (totals, runway), never raw transactions |
| Data | On the phone only, encrypted (Room + SQLCipher). SMS read on-device; only known bank sender IDs; raw text never uploaded |
| Demo | Five sample personas and sample bank SMS on by default. Real SMS behind a switch in Settings |
| Perks | Shelf inside Goals plus an occasional card on Today. Made-up brands only, each perk shows its saving in days |
| Never | Hold money, show ads, sell data, shame spending, recommend specific investments, block a purchase |
| Old design | The earlier HTML prototype ("Ruvo — Student money companion") is a separate older version. Do not copy its screens, tabs or styling |

## Modules

| Module | Kind | What it holds |
| --- | --- | --- |
| `core/engine` | Plain Kotlin | Runway maths, buckets, daily-spend rule, Before you buy, splits, money arrives, day strip, Indian formatting, demo personas |
| `core/sms` | Plain Kotlin | SMS parser, merchant dictionary that learns, duplicate merging, sample SMS set |
| `core/design` | Android library | Tokens, two themes, typography, custom icons, shared components (cards, rolling digits, sheets, dock) |
| `core/data` | Android library | Room + SQLCipher database, repository, settings, demo seeding, SMS receiver and inbox scan, app graph (manual DI) |
| `feature/today` `feature/money` `feature/goals` | Android library | One module per tab |
| `feature/orb` | Android library | The Orb, chat, scripted answers, optional live AI and TTS |
| `feature/onboarding` | Android library | 90-second first run |
| `widget` | Android library | Glance home-screen widget and Quick Settings tile |
| `app` | Application | Activity, navigation, dock, Orb overlay, sheets |

Dependency injection is a small hand-written graph (`RuvoGraph` in core/data) instead of Hilt, to keep the
build simple. Switch to Hilt later only if Kumkum asks.

## Build machine

Kumkum's Windows PC (DESKTOP-73AVRJU). Project folder: `C:\dev\RUVO` (outside OneDrive, plain English path).
If the repo is still inside OneDrive's `문서` folder, move it to `C:\dev\RUVO` before the first build.

## Commands (PowerShell, from the project root)

```powershell
.\gradlew.bat :core:engine:test :core:sms:test   # maths and parser, no emulator, seconds
.\gradlew.bat test                               # every unit test
.\gradlew.bat assembleDebug                      # build the APK
.\gradlew.bat installDebug                       # install on the running emulator
.\gradlew.bat connectedCheck                     # UI tests on the emulator
adb shell am start -n com.ruvo.app/.MainActivity # launch
adb emu sms send 5554 "Rs.180.00 debited from a/c **1234 on 06-10-26 to VPA uber.india@axisbank"  # test SMS (turn on demo SMS in Settings first)
adb exec-out screencap -p > artifacts\screen.png  # screenshot
```

## Status

| Milestone | State |
| --- | --- |
| 0 Design | Style chosen by Claude under full creative liberty (Daybreak / Night swim, Anek Latin, liquid Orb). Kumkum reviews on the emulator |
| 1 Foundation | Written, not yet compiled: modules, tokens, two themes, custom icons, floating dock, navigation |
| 2 Engine and data | `core/engine` **done and tested** (runway maths, buckets, daily-spend rule, Before you buy, splits, day strip, personas). Room + SQLCipher layer written, not yet compiled |
| 3 Onboarding and Today | Written, not yet compiled: 90-second first run with reveal, Today with hero, day strip, buckets, lesson and perk cards |
| 4 Capture | `core/sms` **done and tested** (parser 100% on 39 sample SMS). Receiver, 90-day scan, notification, quick-add and swipe sort stack written, not yet compiled |
| 5 Money | Written, not yet compiled: activity, buckets with Paid, Owed to you with UPI link, transaction sheet with split |
| 6 Goals and Perks | Written, not yet compiled: liquid jars with tilt, pour and release, new goal, perks shelf, monthly review |
| 7 Orb and Ruvo | Scripted voice **done and tested** (20 common questions). AGSL Orb with canvas fallback, chat, live AI, TTS written, not yet compiled |
| 8 Outside the app | Written, not yet compiled: Glance widget, Quick Settings tile, app shortcuts, 8 lessons |
| 9 Polish and proof | Not started |

Tests that already pass (run in the cloud with a stand-in JUnit): `core/engine` 61, `core/sms` 11.

The Android modules were written in a cloud workspace that cannot reach Google's Maven, so they have never
been compiled. **First job on the PC: run `.\gradlew.bat assembleDebug`, fix every compile error, then run
`.\gradlew.bat test` and `.\gradlew.bat connectedCheck` (SmokeTest).** Keep fixes minimal and in the spirit of
the existing code. Update this table as milestones pass.

### Decisions made while writing the code (tell Kumkum if any need changing)

- Hilt replaced by `RuvoGraph`, a small hand-written graph, to cut build complexity.
- `core/sms` is plain Kotlin; the Android SMS receiver and inbox scan live in `core/data/sms`.
- Ruvo's scripted voice, the 8 lessons and the perks are plain Kotlin in `core/engine/guide`, so they are tested.
- Balance = the amount entered at a point in time plus every transaction recorded after it. Past SMS from the
  90-day scan do not move it. Users can reset it from Money → "You have".
- A past bill date only stays reserved in Fixed when we know it was missed; with no payment history it is
  assumed handled (avoids new users seeing last month's rent).
- The Orb floats above the right end of the dock, high enough to be pulled down 64 dp to quick-add.
- Versions pinned in `gradle/libs.versions.toml` (AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12.01) were chosen
  without network access. If Android Studio offers upgrades, accept them only after the first green build.

## How to work here

- Every change ends with `.\gradlew.bat test` green. Engine or parser changes need a test first.
- Money is always `Long` paise (`Paise`). Never use Float or Double for amounts except display maths.
- Use `IndianFormat` for every amount and date on screen.
- Colours, type and shapes come only from `core/design` tokens. No hard-coded colours in features.
- Motion: springs, not timed curves. Respect "remove animations" (`LocalReducedMotion`).
- Touch targets ≥ 48 dp; every number has a TalkBack description (`IndianFormat.spoken`).
- Never commit `local.properties` (it holds `RUVO_CLAUDE_API_KEY` if Kumkum adds one).
- Commit after each passing step with a short message saying what changed.
- At the end of each milestone, take emulator screenshots in both themes into `artifacts/` and stop for
  Kumkum's approval before starting the next milestone.
