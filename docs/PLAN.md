> Exported from the Claude Docs planning doc "Ruvo — Implementation Planning Q&A" on 3 Nov 2026.
> The **Final plan — locked** section at the end overrides anything earlier that disagrees with it.

# Ruvo — Implementation Planning Q&A

 · 

## How to use this doc

No code gets written until every question below has your answer. Each part is a table: the question, my recommendation, the alternatives, and an empty **Your answer** column for you to type in. Comment on any cell if you want to argue a point; that's the two-sided part.

**What "starting from zero" means here.** I'm discarding the old screens, IA, flows and visual style. I'm keeping the student research (9 interviews, 37 survey responses) as evidence, because the findings are still true. If you want the research discarded too, say so in Part 2.

**The planning sequence.** Four gates, each closed before the next opens:

- **Gate A — Concept locked** (Parts 1–4): what Ruvo is, who it's for, the creative direction, MVP scope.
- **Gate B — Experience locked** (Parts 5–8): IA, flows, motion and visual system, tested as a clickable prototype with real students.
- **Gate C — Build plan locked** (Parts 9–13): data, Android architecture, privacy, AI, accessibility.
- **Gate D — Ready to code** (Parts 14–15): test plan, roadmap, definition of done.

The fastest way through: answer the **Decision log** at the end first. Those eight questions unlock most of the rest.

## Part 1 — Vision and positioning

My recommendation: Ruvo is a money-runway app for Indian students. It tells you how many days your money will last, and helps you make it last longer. Everything else serves that one idea.

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 1.1 | What is Ruvo in one sentence? | "Ruvo shows students how many days their money will last, and helps them stretch it." | "An all-in-one student finance app" (broader, forgettable) | i will like to go with the alternative to creat ean overall all in one app |
| 1.2 | What is the one number on the home screen? | **Buffer days** : days of money left minus days until next income. +4 means you'll make it with 4 days spare. | Runway days alone; safe-to-spend today in ₹; account balance | **Buffer days** : days of money left minus days until next income. +4 means you'll make it with 4 days spare. |
| 1.3 | Who is it for at launch? | Students aged 18–25 in India living away from home (hostel, PG, shared flat) | All students; students plus young professionals | Students aged 18–25 in India living away from home (hostel, PG, shared flat) |
| 1.4 | What makes it impressive by US design standards? | One bold idea executed with craft: time instead of balance, physical motion, home-screen widgets, a calm and non-judgemental voice. Craft benchmarks: Copilot Money, Monzo. | Feature breadth (the Finvex route) | One bold idea executed with craft: time instead of balance, physical motion, home-screen widgets, a calm and non-judgemental voice. Craft benchmarks: Copilot Money, Monzo. |
| 1.5 | What is the end goal of this project? | Decide: a portfolio-grade prototype, or an app actually published on Play Store. This changes Parts 9–12 a lot. | Portfolio prototype; Play Store launch; both in sequence | i dont want to actul ly publish the app onto the playstore but create an complete protfolio grade project |
| 1.6 | What will Ruvo never do? | Never hold money or act as a bank. Never sell data or show ads. Never shame spending. Never recommend specific investments. | Allow ads or partner offers later | Never hold money or act as a bank. Never sell data or show ads. Never shame spending. Never recommend specific investments. |
| 1.7 | How does it make money? | Defer. Free core for MVP; revisit after beta. | Freemium subscription; student partner offers | Freemium subscription; student partner offers |

## Part 2 — Users and what we keep from the research

We design for three archetypes, not five student types. Income cadence separates them better than where they live, and cadence is what changes the runway maths.

| Archetype | Lives in | Money arrives | Core worry | What Ruvo does for them |
|---|---|---|---|---|
| Aarav, "Monthly allowance", 19 | Hostel | Fixed amount from family, monthly | Runs out in the last week | Daily safe-to-spend, early warning |
| Meera, "Shared flat", 22 | PG or flat | Internship plus family top-up | Rent, bills and who-owes-whom | Fixed costs set aside first, split as a gesture |
| Kabir, "Lump sum", 21 | Hostel | Scholarship per semester, or irregular part-time pay | Spending too much early | Stretches one big amount over months |

**Research findings we carry forward** (from your deck): tracking isn't understanding; budgets depend on income type; small expenses add up; shared costs cause friction; saving comes last; the real need is control. The two strongest survey numbers: 40.5% struggle to control unnecessary spending, 35.1% don't know how much they can safely spend.

**Gaps the research doesn't answer yet:** how much spending is cash; whether students will grant SMS or bank-data permission; whether "days" makes more sense to them than "rupees".

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 2.1 | Are three archetypes enough? | Yes. Family-home students are a later segment; their need is lowest in your matrix. | Five types as in the deck | Five types as in the deck |
| 2.2 | More research before design? | No more surveys. Run 5 short concept tests in week 1: show the buffer-days idea on paper and check if students get it. | New survey; skip research entirely | do whatecver researh you yourself can and improve up o n dont wait for any new survery f o r me to do |
| 2.3 | Keep the old research as evidence? | Yes, for the findings above. | Discard and restart research | yes do keep the old research |

## Part 3 — Core concept and creative direction

I recommend Direction B as the structure, with Direction A's physical feel as the delight layer. The working name for the concept is **"Days, not rupees."**

**Direction A — The Tank.** Your money is liquid in a vessel. Spending drains it, income fills it, and the liquid sloshes when you tilt the phone. Memorable and very tactile. Risk: reads as a gimmick if it carries information badly.

**Direction B — The Calendar.** Home is a strip of days running to your next income. Each day shows its share of money, and you can see the exact day money runs out. Clear, novel, and directly answers "how much can I safely spend." Risk: needs careful design to feel warm, not like a spreadsheet.

**Direction C — The Companion.** A character lives on the home screen and its world reflects your finances; you mostly talk to it. Risk: can feel childish to a 22-year-old, and numbers get buried.

**The core mechanics, whichever direction wins:**
- **Three buckets.** Fixed (already promised: rent, mess, recharge, subscriptions). Fuel (free to spend). Future (protected for goals).
- **Buffer days.** The headline number, calculated as below.
- **Weekly sort.** Detected transactions collect in a stack; a 2-minute weekly session confirms them.
- **Before you buy.** Type a price, see what it costs in days.
- **Split as a gesture.** Mark any transaction as shared; Ruvo tracks who owes you.
- **Cadence modes.** Monthly, lump sum, or irregular income, set at onboarding.

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 3.1 | Which creative direction? | B structure plus A's physicality | A alone; C alone; a new direction you propose | b+ a |
| 3.2 | Headline wording? | "Buffer days" for the number, phrased as "You'll make it with 4 days to spare" | "Runway"; "Days left" | "Runway"; "Days left" |
| 3.3 | Keep bucket names Fixed / Fuel / Future? | Yes; short, alliterative, easy to remember | Bills / Spend / Save; your own names | Yes; short, alliterative, easy to remember |
| 3.4 | Keep the mascot? | Keep, but as a small status companion, never the main UI. It never looks sad at the user's spending. | Make it central; drop it | Keep, but as a small status companion, never the main UI. It never looks sad at the user's spending. i need you to create something new in thi s its highly interactive but doesnt seem childish |
| 3.5 | Voice and tone? | Calm, peer-like Indian English. Plain words, no guilt, light humour only when things are going well. | Playful and cheeky throughout; formal | keep the voice indian femal e english to ne w i th high maturity an d insi ghtful convection |
| 3.6 | Average daily spend: from history or from a plan? | History (last 14 days), with a plan fallback for new users | Plan only; history only | try keeping both but selce ction of wh ich shall be done automatially by the app on a rule based sntax keeping an option to change whenever user wants |

## Part 4 — MVP scope

The MVP is 12 features that all serve buffer days; everything else waits for evidence from the beta. Change any Scope chip to move a feature.

| Feature | Scope | Why |
|---|---|---|
| Onboarding: income cadence, next income date, fixed costs | MVP | Buffer days can't be calculated without it |
| Home: buffer days and the day strip | MVP | The core idea |
| Manual quick-add in 3 taps or fewer | MVP | Cash exists; works on every phone |
| SMS auto-capture (Android) | MVP | Removes most logging; needs Play approval, and the app must fully work without it |
| Weekly sort stack | MVP | Replaces daily logging with one short ritual |
| Three buckets: Fixed, Fuel, Future | MVP | Gives every rupee a job, simply |
| Goals with protected money | MVP | Saving first, not last |
| Before you buy | MVP | Acts before spending, not after |
| Split as a gesture plus UPI payment link | MVP | Covers shared costs without a Splitwise clone |
| Home-screen widget and Quick Settings tile | MVP | Daily use happens outside the app |
| Contextual micro-lessons (8 to start) | MVP | Teaching inside daily use, per your brief |
| Monthly review, one screen | MVP | Closes the loop: review, learn, improve |
| Share-to-Ruvo import of receipts and screenshots | Later | Good fallback if SMS is denied; also the iOS route |
| s ms synking for seeing how much spent directly sms reador of how much spent an d whatever the sms c an be used for the bedst | MVP | Needs a regulated partner and per-fetch fees |
| Cloud backup and sync | Later | Needs accounts and a backend |
| Hindi UI | Later | After English copy is proven |
| AI assistant | MVP | Needs the data layer working first |
| Lesson library ("books") | Later | Content-heavy; lessons first |
| iOS app | Later | Android first |
| Full group ledger, Splitwise-style | Cut | Your test feedback said not needed |
| Marketplace and student offers | MVP | Distraction from the core; revisit for revenue |
| Investment tracking | Cut | Not a student need yet; regulatory risk |

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 4.1 | Do you agree with the MVP list? | Yes as above | Move items using the chips | i added what i want |
| 4.2 | Accounts and login at MVP? | No login. Data stays on the phone, protected by fingerprint lock. Open the app and see your buffer days in under 90 seconds. | Phone-number OTP login from day one | noo needed before publishing |
| 4.3 | If Play rejects SMS access, what's the fallback? | Promote share-to-Ruvo import into MVP | Manual only until AA arrives | not needed now |

## Part 5 — Information architecture and navigation

Three destinations instead of five: **Today**, **Money** and **Goals**. Lessons appear in context rather than as a tab, and capture is available from everywhere, including outside the app.

Settings (app lock, the Learn list, export) sits behind the profile icon on Today and is left out of the drawing.

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 5.1 | How many top-level destinations? | Three: Today, Money, Goals | Four with a Learn tab; five as before |  |
| 5.2 | Navigation pattern? | Material 3 bottom bar with three items, plus a floating capture button | No tab bar: one vertical home with swipe-up activity and pull-down capture (bolder, riskier) |  |
| 5.3 | Where do lessons live? | Shown in context (a lesson card appears when a related pattern happens), plus a "Learn" list inside Settings | Their own tab |  |
| 5.4 | Where does split live? | On the transaction itself, plus an "Owed to you" row on Money | A Split tab |  |
| 5.5 | Capture entry points? | In-app button, widget, Quick Settings tile, long-press app shortcut, the SMS notification | In-app only |  |

## Part 6 — Key user flows

Eight flows make up the MVP. The first one sets the bar: a new user sees their buffer days in under 90 seconds, with no account.

**1. First run (under 90 seconds)**

- One welcome screen: "See how many days your money lasts."
- How does money reach you? Monthly / One big amount / It varies.
- How much do you have right now? (one number)
- When does the next money arrive? (date picker; skipped for "it varies")
- What's already promised? Rent, mess, recharge, subscriptions as tappable chips with amounts.
- Reveal: the day strip animates in and shows buffer days.
- Optional, after the reveal: turn on auto-tracking (SMS permission with a plain explanation), add a goal.

**2. Spend captured automatically.** A bank SMS arrives → Ruvo parses it on the phone → a quiet notification shows "₹180 Swiggy, Fuel" with a one-tap "Shared?" action → it waits in the weekly sort stack.

**3. Cash quick-add.** Tap widget or tile → type amount → pick from 4 suggested categories → done. Three taps.

**4. Weekly sort (Sunday evening).** A card stack of the week's detected transactions. Swipe right to confirm, left to ignore, up to mark shared, tap to re-bucket. Ends with a one-line summary of the week.

**5. Before you buy.** Type a price → see "costs 3 days; you'd still make it with 1 day to spare" → buy or skip. Ruvo never blocks a purchase.

**6. Split.** Mark a transaction shared → pick people from contacts (system picker, no contacts permission) → Ruvo sends a UPI payment link via WhatsApp or SMS → you mark it paid when it arrives.

**7. Money arrives.** Income detected or entered → Ruvo offers to set aside Fixed first, then a suggested amount for Future → buffer days recalculates.

**8. Running low.** Buffer days drops below zero → one calm notification with two concrete options (e.g. "₹150 a day gets you to the 1st"). Never more than one warning per day.

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 6.1 | Ask for SMS permission during onboarding or after? | After the reveal, so the value is clear first | During onboarding; only from settings |  |
| 6.2 | Weekly sort day and time? | Sunday 7 pm by default, user can change | Daily; whenever the stack has 10+ items |  |
| 6.3 | Notification ceiling? | Max 2 per day, quiet hours 11 pm–8 am | No ceiling; user-set |  |
| 6.4 | Split settlement? | UPI payment link only; Ruvo never moves money | Collect requests via a payments partner |  |
| 6.5 | Should "Before you buy" ever say no? | Never blocks; shows consequences only | Soft block above a threshold |  |

## Part 7 — Interaction, motion and haptics

This is where Ruvo earns "impressive." Money should feel physical: it has weight when you move it, it settles when you protect it, and the app responds to your hand rather than playing canned animations.

**Principles**
- **Springs, not timed curves.** Everything that moves is driven by spring physics, so it can be interrupted mid-motion and still feel natural.
- **Every number change is visible.** When buffer days changes, the digits roll to the new value; nothing just swaps.
- **Motion explains cause.** A spend visibly drains the day strip from today forward; income visibly fills it.
- **Haptics carry meaning.** A small vocabulary, used consistently, never decoratively.
- **Respect reduced motion.** With Android's "remove animations" setting on, everything becomes instant fades.

| Interaction | Motion | Haptic | What it tells the user |
|---|---|---|---|
| Buffer days changes | Digits roll; the day strip re-flows | None | The number is alive and current |
| Confirm a transaction in the sort stack | Card flicks away with your swipe speed | Light tick | Done, next |
| Mark a transaction shared | Card splits into pieces upward | Two light ticks | It's now divided |
| Drag money from Fuel to a goal | Money "pours" between bucket shapes, liquid levels change | Rising texture, firm click at release | You moved real value; it's protected now |
| Before you buy result | Today's slice of the strip shrinks by the cost in days | Medium tick | Consequence, made visible |
| Buffer goes negative | Strip end turns a warning colour, gently pulses once | Double tick | Pay attention, calmly |
| Income arrives | Strip refills from left to right | Soft long buzz | Relief |
| Tilt the phone on Goals | Liquid in goal jars sloshes slightly | None | Delight only; off by default with reduced motion |

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 7.1 | How far do we push physics? | Springs and liquid fills everywhere; tilt only on Goals | No tilt at all; tilt on home too |  |
| 7.2 | Sound? | None by default | Subtle sounds for income and goals |  |
| 7.3 | Use Android 14+ predictive back and edge-to-edge? | Yes; it signals a modern, native app | Standard back only |  |
| 7.4 | Performance target? | Smooth on a ₹10–15k phone (no dropped frames in the sort stack and strip) | Flagship phones only |  |

## Part 8 — Visual design system

Nothing in the visual style is locked until you approve it. My proposal: a calm, paper-and-ink base with one colour per bucket, so colour always means something. This also answers the "white plus black UI" feedback from your testing round.

**Three style directions I'll mock up for you to choose from:**

- **Paper and ink.** Warm off-white, near-black type, three bucket colours only. Editorial, confident, very un-fintech. My recommendation.
- **Night neon.** Deep dark base with vivid accents, closest to the previous Ruvo. Energetic, but common in Indian fintech.
- **Material Expressive.** Android's newest style: bold shapes, dynamic colour from the user's wallpaper. Most native-feeling, least distinctive.

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 8.1 | Base style? | Paper and ink, with a full dark mode | Night neon; Material Expressive | i nees both light and dark modes with different vibes with switch buttons |
| 8.2 | Colour meaning? | Fixed, Fuel, Future each get one colour used nowhere else; one warning colour; everything else neutral | Brand palette used freely | b rand palette used freely |
| 8.3 | Typeface? | One characterful sans with tabular (equal-width) numerals so rolling digits don't jitter; must include the ₹ sign | Keep Outfit; system font (Roboto Flex) | bring new creativ echoice making an ove rall brand consistancy |
| 8.4 | Numbers style? | Indian grouping (₹1,25,000), large display numerals for buffer days | Western grouping | ind |
| 8.5 | Shapes? | Soft rounded rectangles; buckets have distinct shapes so colour is never the only cue | All cards identical | All cards identical |
| 8.6 | Iconography? | Material Symbols, rounded, one weight | Custom icon set (more time) | Custom icon set (more time) no traditional emoji |
| 8.7 | Mascot style? | Redrawn to match the chosen style; small, abstract, few expressions | Keep the old purple character | take full creative liberty |
| 8.8 | Logo and name? | Keep the name Ruvo; revisit the logo once the style is chosen | Rename; keep the old logo | Keep the name Ruvo; revisit the logo once the style is chosen |
| 8.9 | How do you approve style? | I build three home screens (one per direction) as design artifacts; you pick and annotate | Moodboards first | Moodboards |

## Part 9 — Money capture and data model

Ruvo gets money data from four sources, ranked by effort for the student. All parsing happens on the phone; raw SMS text is never uploaded.

| Source | Platform | Effort for user | Status and constraint |
|---|---|---|---|
| Bank and UPI SMS | Android only | None | Google Play allows "SMS-based money management" as an exception for non-default SMS apps, subject to review; it's described as temporary. Needs a permissions declaration and prominent disclosure. Play policy |
| Manual quick-add | Both | 3 taps | Always available; the only route for cash |
| Share-to-Ruvo (receipt or payment screenshot) | Both | 2 taps | On-device text recognition reads the amount and merchant |
| Account Aggregator bank link | Both | One consent flow | Later. Data recipients in the AA network are regulated entities, so we'd need a licensed partner. Per-fetch pricing became standard on 1 Nov 2026. CA Alley |

**Data model (first draft)**

| Entity | Key fields |
|---|---|
| Income plan | Cadence (monthly, lump sum, irregular), expected amount, next date |
| Commitment | Name, amount, due day, recurring or one-off (feeds Fixed) |
| Transaction | Amount, direction, merchant, date, source (SMS, manual, import), category, bucket, status (to sort, confirmed, ignored) |
| Bucket | Fixed, Fuel, Future; computed balances |
| Goal | Name, target, protected amount, target date, jar shape |
| Split | Transaction, people, share each, paid or pending |
| Lesson progress | Lesson id, seen, completed, trigger that showed it |
| Snapshot | Daily buffer-days value (for the monthly review chart) |

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 9.1 | Where is data stored? | On the phone only, encrypted, for MVP | Cloud from day one |  |
| 9.2 | How are transactions categorised? | On-device merchant dictionary (Swiggy → Food) plus learning from the user's corrections | Cloud AI categorisation |  |
| 9.3 | Which banks' SMS formats do we support first? | The 10 banks most used by students in our beta (we'll ask testers) | All banks at once |  |
| 9.4 | Duplicate handling (SMS from bank and UPI app for the same payment)? | Match on amount + time window + reference number; merge silently | Ask the user each time |  |
| 9.5 | Back up or export? | CSV export at MVP; Google Drive backup later | Nothing at MVP |  |

## Part 10 — Android technical architecture

My recommendation is native Android: Kotlin with Jetpack Compose. The physics in Part 7, the widget, the Quick Settings tile and SMS handling are all strongest natively. Business logic (the runway engine, parsing, data) is written in Kotlin Multiplatform so iOS can reuse it later.

The widget and tile also open Capture directly; a server only appears when sync, bank linking or the assistant arrive.

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 10.1 | Framework? | Kotlin + Jetpack Compose, shared logic in Kotlin Multiplatform | Flutter (one codebase, good animation, weaker native widgets and SMS); React Native |  |
| 10.2 | iOS later: shared UI or native UI? | Native SwiftUI on top of the shared Kotlin logic | Compose Multiplatform for both |  |
| 10.3 | Local database? | SQLDelight (works with Multiplatform), encrypted | Room (Android only) |  |
| 10.4 | Widget technology? | Jetpack Glance | Classic RemoteViews |  |
| 10.5 | Oldest Android version supported? | Android 8.0 (API 26), to cover budget phones students use | Android 10+ |  |
| 10.6 | Backend at MVP? | None. A backend arrives with sync, AA and AI. | Firebase from day one |  |
| 10.7 | Who builds it? | Tell me: you alone, with me pairing, or a team? This sets the timeline in Part 15. | — |  |

## Part 11 — Privacy, security and compliance

Trust is a design feature here: students are handing over their bank messages. The rule is that Ruvo reads only what it needs, keeps it on the phone, and says so in plain words at the moment it asks.
- **SMS:** read only messages from bank and UPI sender IDs; ignore personal messages entirely; never upload raw text. Play's policy explicitly forbids budgeting apps from sharing non-financial or personal SMS.
- **Encryption:** database encrypted with a key held in the Android Keystore; app lock with fingerprint or PIN.
- **No tracking SDKs, no ads.** Crash reporting only, with no personal data.
- **India's DPDP Act:** plain-language consent, a delete-everything button, and purpose limitation. Under-18 users need verifiable parental consent under the Act, so launch 18+ only. Check the current DPDP Rules timeline before the beta.
- **Financial advice line:** lessons are education; Ruvo explains the user's own numbers but never recommends specific funds, stocks or loans.

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 11.1 | Minimum age? | 18+ at launch | Allow under-18 with parental consent |  |
| 11.2 | App lock on by default? | Offered at onboarding end, on by default | Off by default |  |
| 11.3 | Analytics? | Privacy-friendly, counts only (e.g. "weekly sort completed"), no amounts or merchants | Full product analytics |  |
| 11.4 | Who writes the privacy policy? | I draft it in plain language; you get it reviewed before Play submission | Use a template generator |  |

## Part 12 — AI assistant

The assistant comes after the MVP, once the data layer is reliable. Its job is narrow: explain the user's own numbers and answer "can I afford this?" in plain words. Most of what the old chatbot did is better as UI (Before you buy, lesson cards).

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 12.1 | When does it ship? | After beta (v1.2) | In MVP | In MVP |
| 12.2 | What can it answer? | Your numbers ("why did buffer days drop?"), affordability, money basics | Anything about finance, including investing | Your numbers ("why did buffer days drop?"), affordability, money basics |
| 12.3 | What does it never do? | Recommend specific investments, loans or products; give tax or legal advice | Allow general investing suggestions | Recommend specific investments, loans or products; give tax or legal advice |
| 12.4 | Where does the model run? | Cloud model, sending only summaries (category totals, buffer days), never raw transactions | On-device model where the phone supports it | keep it easy for demo take free choice o n how |
| 12.5 | How does it show up? | Chat opens from "Why?" links next to numbers, not a floating bubble on every screen | Floating button everywhere, as before | Floating button everywhere, as before |
| 12.6 | Does the maths come from the AI? | No. The runway engine calculates; the AI only explains its results | AI computes answers | No. The runway engine calculates; the AI only explains its results |

## Part 13 — Accessibility and localisation

Accessibility is built in from the first screen, not audited at the end. US design reviewers judge this closely, and it's cheap when planned early.
- **TalkBack:** every number reads naturally ("twelve thousand and seventy-one rupees", "4 days to spare"); the day strip has a text summary.
- **Text size:** layouts hold up at 200% font scale; buffer days never truncates.
- **Contrast:** WCAG AA minimum for all text, including on bucket colours.
- **Colour is never alone:** buckets also differ by shape and label.
- **Touch targets:** 48 dp minimum.
- **Reduced motion:** honoured everywhere (Part 7).

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 13.1 | Launch language? | English, written for Indian students; all text ready for translation | English and Hindi at launch |  |
| 13.2 | Second language? | Hindi in v1.1, then based on beta demand | Marathi or Tamil next |  |
| 13.3 | Date format? | 3 Nov, 3 Nov 2026; weeks start Monday | 11/03 style |  |
| 13.4 | Accessibility target? | WCAG 2.2 AA | Best effort |  |

## Part 14 — Testing and validation

We test the idea before the screens, and the screens before the code. Each round has a pass bar, proposed below; change any number you disagree with.

| Round | When | Who | What we test | Pass bar (proposed) |
|---|---|---|---|---|
| Concept test | Before Gate A | 5 students, one per archetype plus 2 | Do "buffer days" and the three buckets make sense on paper? | 4 of 5 explain buffer days back correctly |
| Prototype round 1 | Before Gate B | 6 students | First run, quick-add, weekly sort on a clickable prototype | First run under 90 s; no task needs help |
| Prototype round 2 | Before Gate B | 6 new students | Same tasks after fixes, plus Before you buy and split | All tasks succeed unaided |
| Design audit | Before Gate B | Me, using the UX audit skill | Contrast, tap targets, labels, consistency on every screen | No critical issues |
| SMS parser test | Before beta | Real bank SMS samples shared with consent | Amount, merchant, date and direction extracted correctly | 95%+ correct |
| Closed beta | After Gate D | 30–50 students, one campus, via Play closed testing | Real use over 4 weeks | Most testers still doing the weekly sort in week 4 |

| # | Question | My recommendation | Alternatives | Your answer |
|---|---|---|---|---|
| 14.1 | Can you recruit students for tests? | Yes, from your college; I'll write the test scripts | Remote testing |  |
| 14.2 | Beta size and place? | 30–50 students on one campus | Several campuses |  |
| 14.3 | Main success metric for beta? | Weekly sort completion in week 4 (shows the habit stuck) | Daily opens; transactions captured |  |

## Part 15 — Roadmap, phases and gates

Superseded by the Final plan at the end of this doc (no beta now). Original: six phases, each closed by a gate. Coding starts only at Phase 3. Durations depend on your answer to 10.7, so they're left off until then.

Each gate's criteria come from Part 14; a gate that fails sends us back into its phase, not forward.

**How we work together in each phase**
- **Planning (now):** this doc. You answer, comment and overrule; I record decisions in the log below.
- **Design:** I build screens as design artifacts you can comment on and edit, using the frontend-design skill for visual direction. Nothing is final without your approval.
- **Review:** the UX audit skill checks every screen for contrast, tap targets and consistency before each test round.
- **Build:** Kotlin and Compose, screen by screen, in the order of the flows in Part 6.

**Definition of done for the MVP:** all 12 MVP features work on a ₹10–15k Android phone; the 8 flows pass a prototype test round; the SMS parser passes its accuracy bar; Play's SMS declaration is submitted; TalkBack and 200% text pass on every screen.

## Decision log — answer these first

These eight answers unlock most of the doc. Answer them here (or in their parts) and I'll mark them Decided; every later change gets marked Changed with a note.

| # | Decision | My recommendation | Status | Your answer |
|---|---|---|---|---|
| 1.5 | Portfolio prototype or real Play Store launch? | Decide first; it sets depth for Parts 9–12 | Decided | portfolio |
| 10.7 | Who builds it, and how many hours a week? | Needed to put dates on the roadmap | Decided | you |
| 1.2 | Buffer days as the hero number? | Yes | Decided | yes |
| 3.1 | Creative direction? | Calendar structure with Tank physicality | Decided | complete creative liberty ensure you dont make any genral design i. need an ui heavy and impressive looking u i with animations and anything that you want but keeping the style acceptqable for the t arget audiance |
| 4.1 | MVP scope as listed? | Yes, 12 features | Changed | yes |
| 4.2 | No login at MVP? | Yes, phone-only data | Decided | nah |
| 8.1 | Visual style to explore first? | Paper and ink, mocked beside the other two | Changed | as per instruced |
| 10.1 | Native Kotlin + Compose? | Yes | Changed | not needed now only andriod shall be displayable in demo |

## Round 2 — what your answers decided

Ruvo is now a portfolio-grade, Android-only, all-in-one student money app, organised around one number, with the AI companion and marketplace in the first build. Not publishing on Play Store also removes the hardest real-world constraint: a sideloaded app can read SMS with a normal permission prompt, no Google review.

**Locked**

| Area | Decision |
|---|---|
| Goal | Portfolio-grade app, installable for demos, not published |
| Builder | Claude builds; you approve every design step |
| Platform | Android only, Kotlin + Jetpack Compose; no iOS layer |
| Positioning | All-in-one, organised around buffer days |
| Users | Five student types from your deck; old research kept; I do the desk research |
| Concept | Day-strip structure with Tank physicality; full creative liberty; UI-heavy and animated, but right for 18–25 |
| Buckets | Fixed, Fuel, Future |
| Daily spend | A rule picks history or plan automatically; the user can override |
| MVP additions | AI assistant, marketplace, deeper SMS use; no login; Account Aggregator dropped |
| Visual | Light and dark themes with different moods and a switch; brand palette used freely; new typeface; Indian number grouping; identical cards; custom icons, no emoji; new mascot; logo revisited; moodboards first |
| Voice | Indian woman's voice in English: mature, warm, insightful |
| AI | Floating button on every screen; explains your numbers, affordability and basics; the engine does the maths; model choice kept simple |

Questions you left blank in Parts 5, 6, 7, 9, 11, 13 and 14 take my recommendation unless you object.

**What "portfolio, not Play Store" changes**
- **SMS becomes core, not risky.** No Play declaration needed. I'll add a demo mode with realistic sample bank SMS, so you never show your real messages in a demo.
- **Privacy (Part 11)** stays as designed screens and behaviour, for credibility, without legal review.
- **Testing (Part 14)** has no beta. It becomes a heuristic review plus the UX audit skill, with an optional quick test on 3 friends.

**Conflicts in the answers, with my proposed fix**

- **All-in-one (1.1) vs one bold idea (1.4).** Fix: "everything a student needs, organised around one number." Every feature feeds or explains runway; a marketplace perk shows "saves you 2 days."
- **Hero wording.** You kept buffer days as the hero but chose "Runway / Days left" as the words; those are two different numbers. Fix: a big "23 days" labelled Runway, with "Next money in 19 days · 4 days spare" beneath.
- **Two floating buttons.** AI on every screen plus a capture button means two floating objects competing. Fix: merge them into the companion (below).
- **"Never show ads" (1.6) vs marketplace and partner offers (1.7).** Fix: Perks are curated, clearly labelled, with no ad networks or tracking. In the demo, every offer is sample content with generic brands, no real logos.
- **Identical cards (8.5) vs colour never alone (Part 13).** Works if every bucket card carries its name and icon, so colour isn't the only cue.
- **Five types (2.1) vs income maths.** Five personas for the story; underneath, the engine uses three income cadences.

**My creative proposal for the companion: the Ruvo Orb**

No cartoon face. A soft liquid sphere that floats on every screen and is the Tank direction turned into a character:
- Its fill level is your Fuel; its surface is calm when runway is healthy and ripples faster when money is tight; it glows when a goal completes.
- It's physical: poke it and it ripples, drag it and it stretches like a droplet, tilt the phone and the liquid sloshes.
- Tap it to talk to Ruvo. Pull it down to quick-add a spend. One object replaces the mascot, the AI button and the capture button.
- Its colour and material change with the light and dark themes.

**New questions**

| # | Question | My recommendation | Your answer |
|---|---|---|---|
| R1 | Do you have Android Studio on your computer, and a phone or emulator for demos? | Needed: this cloud workspace can't reach Google's Android download servers, so I can't compile the app here. I write the code, put it in a folder on your computer, and you press Run (or I run the build there, if the link to your computer allows). |  |
| R2 | Hero wording: "Runway 23 days" with "4 days spare" beneath? | Yes |  |
| R3 | Merge mascot, AI button and capture into the Orb? | Yes |  |
| R4 | Orb: pure shape, or two small light-points as eyes for attention cues? | Pure shape; motion carries the emotion |  |
| R5 | Ruvo's voice: written only, or also spoken in the AI chat? | Written persona at first; spoken reply using the phone's Indian English text-to-speech as an optional toggle |  |
| R6 | AI model for the demo? | A cloud model through an API key you provide, plus scripted offline answers so the demo never breaks without internet |  |
| R7 | Demo data? | Sample persona and sample SMS on by default; real SMS behind a switch |  |
| R8 | Where do Perks (marketplace) live? | A Perks section in Goals ("ways to save and earn"), plus occasional cards on Today |  |
| R9 | Final portfolio package? | Installable app, a 60–90 s screen recording, and an updated case-study deck |  |

**Next, once these are answered:** desk research (competitor onboarding and home screens, student spending patterns, bank SMS formats), then moodboards for both themes, the typeface and the Orb, for your approval.

## Final plan — locked

The plan is final: Ruvo is a native Android portfolio app, built and tested on your Windows PC (DESKTOP-73AVRJU) in ten milestones, each approved by you before the next starts. Design comes first and no code is written before you approve the moodboards and key screens. The old Ruvo prototype and its visual style are discarded; nothing from them carries over. Where this section differs from earlier parts, this section wins.

| # | Question | Final decision |
|---|---|---|
| R1 | Where do we build and test? | Your Windows PC (DESKTOP-73AVRJU), in the folder C:\dev\RUVO. Claude Code on the PC runs the builds, tests and emulator; Android Studio is there for you to inspect and profile . An Android phone is optional, for the final check of haptics and tilt. |
| R2 | Hero wording | Big "23 days" labelled Runway; "Next money in 19 days · 4 days spare" beneath |
| R3 | One companion | The Ruvo Orb replaces the mascot, the AI button and the capture button |
| R4 | Orb face | Pure shape, no eyes; motion, light and ripples carry emotion |
| R5 | Ruvo's voice | Written persona everywhere; spoken replies in the chat as an option, using the phone's Indian English voice |
| R6 | AI for the demo | Scripted offline answers by default, so the demo never fails. Live AI through the Claude API when you add your own key; the key stays in a local file on your PC and never goes into the code repository. |
| R7 | Demo data | Five sample personas and realistic sample bank SMS, on by default. Real SMS behind a switch in Settings. |
| R8 | Perks | A Perks shelf inside Goals, plus an occasional card on Today. Made-up brands only. Each perk shows its saving in days. |
| R9 | Portfolio package | Installable app (APK), a 60–90 s screen recording, and a new case-study deck |
| 5.1–5.2 | Navigation | Three destinations (Today, Money, Goals) on a custom floating dock, with the Orb floating above it. Not a stock Material bar. |
| 5.3–5.5 | Lessons, split, capture | Lessons in context plus a Learn list; split lives on the transaction; capture from the Orb, widget, Quick Settings tile, app shortcut and SMS notification |
| 6.1–6.5 | Flows | SMS permission asked after the reveal; weekly sort Sunday 7 pm; max 2 notifications a day; UPI payment links only; Before you buy never blocks |
| 7.1–7.4 | Motion | Springs everywhere; tilt on the Orb and goal jars; sound off by default; edge-to-edge and predictive back; smooth on a mid-range phone profile |
| 9.1–9.5 | Data | On the phone only, encrypted; merchant dictionary that learns from corrections; 10 common bank SMS formats plus UPI; silent duplicate merging; CSV export |
| 10.x | Platform | Android only (Part 10's Multiplatform layer is dropped); minimum Android 8.0; no backend |
| 11.x | Privacy | 18+ confirmation at onboarding; app lock offered at the end of onboarding; no analytics; a plain-language privacy screen in the app |
| 13.x | Language and accessibility | English with Indian number grouping and Indian date style (3 Nov); WCAG 2.2 AA |
| 14.x | Testing | Replaced by the test plan below (no beta) |

### Tech stack and project structure

Everything is standard, current Android tooling, so the code reads as professional work to anyone reviewing the portfolio.

| Layer | Choice | Why |
|---|---|---|
| Language and UI | Kotlin, Jetpack Compose, custom design system on top of Material 3 | Compose is where spring physics, custom drawing and theming are strongest |
| Orb and liquid effects | AGSL shaders on Android 13+, simpler Canvas drawing on older phones | Real-time liquid surface that reacts to touch and tilt |
| Motion and haptics | Compose spring animations, the phone's motion sensor, Android's haptic effects | The physics in Part 7 |
| Architecture | Screen state flows from a view model per screen; Hilt for wiring | Easy to test; familiar to reviewers |
| Storage | Room database, encrypted with SQLCipher; DataStore for settings | Android-only now, so Room replaces SQLDelight |
| SMS | A receiver for incoming SMS plus a one-time scan of the last 90 days, parsed on the phone | Core feature now that Play rules don't apply |
| Background work | WorkManager for the weekly sort reminder and the daily runway snapshot | Survives restarts and battery saving |
| Widget and tile | Jetpack Glance widget; Quick Settings tile | Ruvo outside the app |
| AI | Scripted answers on the phone; optional Claude API call with a summary of your numbers (never raw transactions) | Never fails in a demo |
| Speech | Android's built-in text-to-speech, Indian English voice | No extra service |

**Project modules** (each part builds and tests on its own):
- **app**: the shell, with navigation, the floating dock and the theme switch
- **core/design**: design tokens, typeface, custom icons, shared components
- **core/engine**: runway maths, buckets, Before you buy, splits; plain Kotlin with no Android code, so it is testable anywhere
- **core/data**: Room with SQLCipher, DataStore settings, the five demo personas
- **core/sms**: the SMS receiver, the 90-day scan, the parser and merchant dictionary; the parser is plain Kotlin
- **feature/today**, **feature/money**, **feature/goals**: one module per tab
- **feature/orb**: the Orb, the chat, scripted answers and optional live AI
- **widget**: the Glance widget, Quick Settings tile and app shortcuts

### How we build and test on your Windows PC

Your Windows PC is the only build machine; the Mac is out of the plan. The project lives in **C:\dev\RUVO**: outside OneDrive, because syncing locks files in the middle of a build, and on a plain English path, because Gradle and the Android tools can fail on non-English folder names.

**Who does what**
- **This chat:** design (milestone 0), the plan, moodboards and key screens as artifacts you comment on.
- **Claude Code on your PC**, opened in C:\dev\RUVO: writes the code, runs the Gradle builds and tests, starts the emulator, installs the app and takes screenshots. It has a real terminal on the PC. This chat can't: its link to your PC reads and writes files but can't reach Google's servers, and its screen control treats terminals and Android Studio as click-only.
- **You:** approve each milestone, and use Android Studio to look around, profile and click through the app.

**The handover.** A CLAUDE.md file in the project root carries the locked plan, so every Claude Code session starts from it. docs/PLAN.md keeps the full copy, and git records every change so any step can be undone.

**Each milestone**

- Claude Code writes the milestone's code and tests.
- It runs the unit and screenshot tests (`gradlew test`), which need no emulator.
- It runs the flow tests on the emulator (`gradlew connectedCheck`).
- It installs the app, walks through the flows and captures screenshots.
- It commits the work to git with a short note of what changed.
- You review the screenshots and a short recording, and approve before the next milestone starts.

**Testing things an emulator can't feel.** The emulator fakes tilt through its virtual sensor controls and receives test SMS through its extended controls, so the Orb, goal jars and SMS capture can all be tested on the PC. Only haptics need a real Android phone; one final check before the screen recording is enough.

### Build order: ten milestones

Milestone 0 is design only; code starts at milestone 1. Each milestone ends with its tests passing on your PC and your approval. The maths and the SMS parser come early because every screen depends on them.

- **Design (no code).** Desk research: competitor onboarding and home screens, student spending patterns, Indian bank SMS formats. Then moodboards for the light and dark moods, typeface, colour, icon style and the Orb. Then high-fidelity designs of the key screens. Done when: you approve the style and the key screens.
- **Foundation.** Project and modules, design-system tokens from the approved style, light and dark themes with the switch, the floating dock, navigation between the three tabs. Done when: the app launches on the emulator and the theme switch works.
- **Runway engine and data.** Buffer-days maths for all three income cadences, buckets, before-you-buy, splits, the encrypted database, the five demo personas. Done when: about 40 engine tests pass, including edge cases (income date already passed, zero spending, negative runway).
- **Onboarding and Today.** The 90-second first run, the day strip, rolling digits, Before you buy. Done when: first run completes in under 90 s and the numbers match the engine tests.
- **Capture.** Quick-add, SMS reading and parsing, demo SMS, the spend notification, the weekly sort stack with swipe physics. Done when: the parser is at least 95% right on the sample SMS set, and an SMS sent to the emulator appears in the stack.
- **Money.** Activity list, buckets, split with a UPI payment link, Owed to you. Done when: a split payment flows through to buckets and runway correctly.
- **Goals and Perks.** Goal jars with tilt, pouring money from Fuel to a goal, the Perks shelf, the monthly review. Done when: protected money is excluded from Fuel everywhere.
- **The Orb and Ruvo.** The liquid Orb with touch, drag and tilt; the chat; scripted answers; optional live AI and spoken replies. Done when: the 20 most common questions get correct answers offline.
- **Outside the app.** Home-screen widget, Quick Settings tile, app shortcuts, the 8 contextual lessons. Done when: the widget updates within a minute of a new spend.
- **Polish and proof.** Accessibility pass, performance on a mid-range phone profile, final design review, screen recording, case-study deck. Done when: the test plan below passes in full.

### Test plan

Five layers of testing, from the maths up to how it feels in the hand. The first three run automatically on your PC; the last two are done by Claude Code and checked by you.

| Layer | Tool | What it checks | Runs on | When |
|---|---|---|---|---|
| Maths and parsing | Unit tests (JUnit) | Runway, buckets, splits, duplicate merging; every sample SMS parsed against its expected result | Your PC , no emulator needed | Every change |
| Visual regression | Screenshot tests (Roborazzi) | Every screen in light and dark, at normal and 200% text size, compared against approved images | Your PC , no emulator needed | Every change after milestone 1 |
| Flows | Compose UI tests | The 8 flows from Part 6, start to finish | Emulator | End of each milestone |
| Accessibility | Built-in accessibility checks in the UI tests, plus a TalkBack walkthrough | Labels, contrast, 48 dp targets, reading order | Emulator | Milestones 3, 6 and 9 |
| Feel and performance | Manual walkthrough and Android Studio's profiler | Spring motion, no dropped frames in the day strip and sort stack, haptics, tilt | Emulator; a real phone for haptics | Milestones 4, 7 and 9 |

The design review uses the same checklist as the UX audit skill (contrast, tap targets, naming consistency, empty states), applied by hand to emulator screenshots, because the skill's own automatic measurements only work on web pages.

### Your setup checklist (Windows PC)

Milestone 0 (design) can start now; these are only needed before milestone 1.
- Turn on **Windows Hypervisor Platform** in "Turn Windows features on or off", then restart, so the emulator runs at full speed.
- Install **Android Studio** (latest stable) with Standard setup. It brings its own Java.
- Create a **Pixel 8 emulator** with the newest Android image (Google APIs, x86_64) and boot it once.
- Install **Git for Windows**, so Claude Code gets a Bash shell and version history.
- Create **C:\dev\RUVO**, add it to this chat, and remove the old empty RUVO folder from OneDrive.
- Install **Claude Code** and start it in C:\dev\RUVO.
- Optional: an Android phone with USB debugging on, for the final haptics check.
- Optional: a Claude API key for live AI answers, kept in a local file on the PC and never committed.
