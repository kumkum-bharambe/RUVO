# Ruvo design review and direction: "Ticket & Ledger"

Reviewed on a OnePlus Nord CE 3 Lite (Android 15), both moods, every tab, 4 Nov 2026.

## What the navy-and-amber version gets right

- One idea leads: the runway number is the biggest thing on Today.
- The three buckets are consistent and always labelled.
- Motion exists and respects "remove animations".
- The Guide reader is genuinely good: one idea per page, a gentle check, a way to act.

## Where it lacks character

| # | Area | Problem | Why it matters |
| --- | --- | --- | --- |
| 1 | Identity | Glass cards, gradient blobs and glowing pill buttons are the default "fintech on Dribbble" look. Nothing says Indian, student, or Ruvo. | A portfolio piece is judged in three seconds. It must be recognisable. |
| 2 | Shape | Everything is the same soft 26 dp blob: cards, buttons, chips, bars, dock. | With one shape there is no hierarchy; actions and content look alike. |
| 3 | Type | One geometric sans (Outfit) for everything; hierarchy comes only from size. Spaced capitals on almost every block. | No voice. The "mature, warm, insightful" persona has no typographic equivalent. |
| 4 | Colour | Amber means "Fuel", "button", "chart", "selected tab" and "highlight" at once. Three loud gradient bucket cards fight the hero. | When one colour means five things it means nothing. |
| 5 | Hero | A gradient card with a wave. Pleasant, forgettable. | The runway should feel like an object you hold. |
| 6 | Density | Today stacks 13 cards of equal weight. | Card soup: the eye has nowhere to rest. |
| 7 | Motion | Every element fades and rises the same way. No moment is memorable. | Signature motion makes a product feel designed. |
| 8 | Dock | A floating pill plus a floating Orb covers content on every screen. | Two floating layers over a long scroll is clutter. |
| 9 | Data | Rounded pills for days and charts; the day strip runs off screen with no cue. | Data looks decorative instead of exact. |

## Direction: Ticket & Ledger

Indian print culture as the visual language: the railway ticket, the *bahi-khata* ledger, the rubber stamp,
newsprint. Money is a journey; your runway is a ticket that tells you how far it takes you.

**Shape.** Crisp, small radii (4–8 dp). Cards are flat paper on paper, separated by ink rules rather than
shadows. Primary actions sit on a hard offset ink shadow that collapses when pressed, like a stamp. Only living
things are round: the Orb and avatars.

**Type.** Two families with a clear job each.
- *Fraunces* (variable serif with optical sizes): numbers, headlines, the voice of Ruvo. Big numbers at the
  largest optical size; headlines a little soft.
- *Space Grotesk*: interface, labels, amounts in lists, with tabular figures.
Both include the ₹ sign natively.

**Palette.** Named after things every Indian student knows.
- Paper `#F4EEE3`, Ink `#1C1A17`.
- Haldi (turmeric) for **Fuel**: yours to spend.
- Peacock for **Future**: protected.
- Neel (indigo) for **Fixed**: already planned.
- Sindoor (vermilion) only for shortfalls and the stamp. Never for blame.
- Night swim becomes ink-blue night with paper-coloured type, and the hero ticket turns to paper.

**Signature moments.**
- The runway ticket: perforated, notched, with a serial number and a stub for the next money date. A rubber
  stamp ("MAKES IT +4" or "3 DAYS SHORT") lands on it with a thud and a haptic.
- Ink-wipe reveals: content is uncovered left to right, like a pen stroke, instead of fading up.
- Stamp-press buttons: the hard shadow collapses as you press.
- The dock is a flat ledger strip on the bottom edge with a sliding ink tab, so only the Orb floats.
- Day strip: punched ticket squares, hatched where Fuel runs short.

**Kept on purpose.** The runway idea, buckets, Orb, Guide, every feature and every number. Only the way they look
and move changes.
