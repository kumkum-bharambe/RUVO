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
| Platform | Android only, Kotlin + Jetpack Compose, min SDK 26, no backend. On-phone account (Kumkum, 4 Nov 2026): name, mobile number and a 4-digit PIN stored only as a salted PBKDF2 hash; asked on every cold start; five wrong tries pause 30 s; forgotten PIN means resetting Ruvo on the phone. No SMS code, because there is no server |
| Navigation | Four tabs (Today, Money, Goals, Guide) on a custom floating dock with a sliding amber pill; not a stock Material bar. Guide added 3 Nov 2026 at Kumkum's request |
| Companion | Ruvo's **pet** (Kumkum, 4 Nov 2026): a glossy liquid orb drawn in `feature/orb/Pet.kt`. Colours swirl inside a glassy sphere, two white pill eyes blink and glance about, a ring of sparkles drifts round it, and it wobbles like jelly. Swirl colours follow the runway (yellow, orange and pink when comfortable; purple, blue and pink when calm; orange, red and pink when short; violet and cyan while thinking). On Home it greets you once per start with a speech bubble. Tap = talk to Ruvo, pull down = quick-add |
| Visual | Palette (Kumkum, 4 Nov 2026): yellow, orange, purple and night blue with white and black. Daybreak is a soft lavender canvas with white cards; buckets are Fuel yellow, Fixed purple, Future orange; shortfalls are rose. Logo (5 Nov 2026): a violet runway narrowing to the horizon with white centre dashes, and a yellow half-sun rising behind it (the day the next money arrives). One review sheet with This week / This month. GPay-style splits (equally or by amount, your share shown live), also in Quick add. Percentages never go past 100; bigger changes are said as "double". Payments go to Fixed or Fuel only. Dock is a dark pill whose chosen tab opens into a yellow pill with its name. Every Guide book has its own illustrated cover. Type and controls two steps smaller than Ruvo 7. Night swim is true black (#000) with no background glow (Kumkum, 4 Nov 2026). Type and cards one step smaller; labels in sentence case, not spaced capitals. Buckets: Fuel amber, Future blue, Fixed lavender, no greens; filled gradient bucket cards of equal height with no coloured edges, each with its name and no icon. Hero is the light "sun" card with amber and lavender glows and a slow sheen. Status chips with pulsing dots glide past as a ticker under the greeting (still under reduced motion): white on Daybreak, black on Night swim. No day strip on Today. Transactions show look-alike brand logo tiles drawn in code (`MerchantLogo.kt`), category icons otherwise. Goals show illustrated scenes (Goa beach, laptop, phone, umbrella, books, mountain) picked from the name instead of jars. No hide button on the Money balance (the Settings switch stays). The old four-point spark icon is gone everywhere. Blue Credited and Received cards; tappable monthly chart in History; black period chips. Floating dock with a sliding amber pill. Ruvo 7 opening (tile pops, bars rise, amber ring, letters rise, tap to skip). Tabs: Home, Money, Goals, Guide. Fixed costs editable in Money. No perks shelf in Goals and no month plan on Home. Guide shows a spine-per-book reading shelf with a read and to-read list, and the Marketplace tile near the top. Visible + Add, richer profile (navy header, this month, mood switch, shortcuts) and one Marketplace sheet opened from the same tile in Profile and Guide, "Save this idea" button in books, premium dark navy splash, three-bar logo with no coin. Custom icons, no emoji. Indian number grouping (₹1,25,000), dates like "3 Nov" |
| Motion | Mature and calm: springs (`Motion.gentle/soft/snappy`), staggered `reveal()` entrances, drifting `AmbientBackdrop`, hero sheen, bobbing and shimmering status chips, count-up amounts, tappable daily pills, 3D page turns in the Guide reader. Every infinite loop uses an infinite transition or `withInfiniteAnimationFrameNanos` so UI tests can go idle. Reduced motion stills everything |
| Typeface | Outfit (variable weight), as in Ruvo 6. Outfit has no ₹, so on Android 10+ each weight falls back to Anek Latin for ₹ only (`Type.kt`). Both downloaded at build time by `core/design` |
| Voice | Written persona: mature, warm, insightful Indian woman's voice in English. Optional spoken replies via the phone's Indian English TTS |
| AI | Scripted offline answers by default so the demo never fails. Optional live Claude API with the user's own key, sending only a summary (totals, runway), never raw transactions |
| Data | On the phone only, encrypted (Room + SQLCipher). SMS read on-device; only known bank sender IDs; raw text never uploaded |
| Demo | Five sample personas and sample bank SMS on by default. Real SMS behind a switch in Settings |
| Perks | Marketplace sheet (tile in Guide and Profile), after Ruvo 7: every listing shows its category, status (Active, Ends in N days, Needs a re-check, Ended), source, last check, end date and time left, with How to get it on tap; shelf inside Goals, plus an occasional card on Today. Made-up brands only, each perk shows its saving in days |
| Guide | 24 five-minute books on six shelves (`core/engine/guide/Library.kt`), seven pages each: idea, situation, why, reflect, try it, a check, apply in Ruvo. No streaks, no named investments. The eight Today lessons are books with the same ids |
| Never | Hold money, show ads, sell data, shame spending, recommend specific investments, block a purchase |
| Old design | The HTML prototype ("Ruvo — Student money companion") is now the style reference (3 Nov 2026): follow its look and features, but do not copy it screen for screen. Its mascot and phone/OTP login stay out; the Orb stays and there is no login |

## Modules

| Module | Kind | What it holds |
| --- | --- | --- |
| `core/engine` | Plain Kotlin | Runway maths, buckets, daily-spend rule, Before you buy, splits, money arrives, day strip, Indian formatting, demo personas, Insights (daily, week, months, mood), Guide library |
| `core/sms` | Plain Kotlin | SMS parser, merchant dictionary that learns, duplicate merging, sample SMS set |
| `core/design` | Android library | Tokens, two themes, typography, custom icons, shared components (cards, rolling digits, sheets, dock) |
| `core/data` | Android library | Room + SQLCipher database, repository, settings, demo seeding, SMS receiver and inbox scan, app graph (manual DI) |
| `feature/today` `feature/money` `feature/goals` `feature/guide` | Android library | One module per tab. Guide holds the shelf, book covers and the book reader |
| `feature/orb` | Android library | The Orb, chat, scripted answers, optional live AI and TTS |
| `feature/onboarding` | Android library | 90-second first run |
| `widget` | Android library | Glance home-screen widget and Quick Settings tile |
| `app` | Application | Activity, navigation, dock, Orb overlay, sheets |

Dependency injection is a small hand-written graph (`RuvoGraph` in core/data) instead of Hilt, to keep the
build simple. Switch to Hilt later only if Kumkum asks.

## Build machine

Kumkum's Windows PC (DESKTOP-73AVRJU). Project folder: `C:\dev\RUVO` (outside OneDrive, plain English path).

- Gradle runs on **JDK 21** (Temurin, `JAVA_HOME` set for the user). Android Studio's bundled JBR is Java 25,
  which Gradle 8.11 / Kotlin 2.1 cannot read. In Android Studio set Settings → Build Tools → Gradle → Gradle JDK
  to the Temurin 21 JDK.
- Test phone: Kumkum's OnePlus Nord CE 3 Lite (CPH2467, Android 15), connected by **Wi-Fi debugging** because
  USB drops out. If `adb devices` is empty, re-pair from Developer options → Wireless debugging.
- No emulator yet. Create a Pixel 8 one before Milestone 4 (`adb emu sms send` only works on an emulator).

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

**First PC build done (3 Nov 2026).** `assembleDebug` green, `test` green (`core/engine` 61, `core/sms` 11),
SmokeTest passes on the OnePlus. Fixes needed: a build-script import, six cross-module smart casts, the Orb clock
(`withInfiniteAnimationFrameNanos`) and a tilt dead-band so a still phone lets the UI go idle. Every module above
now compiles and the app runs, but each milestone still needs its own on-device check and Kumkum's review.
**Redesign (3 Nov 2026):** navy and amber design system, Outfit, Guide tab, and the HTML prototype's missing
features (weekly review, insights, plan card, coming up, Money overview and history, goal insights, settings extras).
Unit tests: `core/engine` 71, `core/sms` 11. Checked on the phone in both themes; SmokeTest passes. Second redesign "Ticket & Ledger" (4 Nov 2026) replaced
navy and amber, Outfit and glass with paper and ink, Fraunces and Space Grotesk, crisp corners and the runway ticket. Update this table as milestones pass.

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
