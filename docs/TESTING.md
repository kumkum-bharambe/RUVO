# Testing Ruvo

A checklist for trying every important feature, with what should happen. It takes about 20 minutes. Start from a
fresh install ([install guide](INSTALL.md)) and choose **Explore with a sample student → Aarav**, so the numbers
below make sense. Exact numbers change with the date, because the sample student's spending is set relative to
today.

| # | Do this | You should see |
| --- | --- | --- |
| 1 | Open Ruvo for the first time | Golden coin animation, then **Create your Ruvo account** |
| 2 | Enter a name, a 10-digit number starting with 6–9, a 4-digit PIN twice | The welcome screens; nothing asks for internet |
| 3 | Type a 9-digit number instead | "Enter a 10-digit number starting with 6, 7, 8 or 9." and you cannot continue |
| 4 | Tap **Explore with a sample student**, pick **Aarav** | Home with "Good morning/afternoon/evening, Aarav" and a runway in days |
| 5 | Tap the runway card or **Why N days?** | The sum: You have − Fixed − Future = Fuel, then Fuel ÷ daily spend = days |
| 6 | On Home, tap **Before you buy**, type 1500 | "Costs N days", runway before → after, and whether you would still make it; **Got it** closes it |
| 7 | Tap **+**, type 250, tap **Food** | The sheet closes; the runway number drops; the payment is first under **Recent** |
| 8 | Tap **+**, type 400, switch on **Shared with friends?**, add a friend, save, tap **Food** | Money → **Owed** lists the friend with their share |
| 9 | On Owed, tap **They paid** | The person moves out of Waiting; your balance goes up by their share |
| 10 | Money → Overview, find an **Unpaid** fixed cost, tap it | It turns **Paid**; the Fixed amount on Home drops |
| 11 | Money → **+ Add a fixed cost**: "Gym", 500, day 10 | It appears in the list; Fixed grows if day 10 is before the next money |
| 12 | Money → Transactions → **Monthly**, then the arrows | Money in and out for each month; payments grouped by day |
| 13 | Money → History, tap another month, tap **Edit** | A form to correct that month's totals |
| 14 | Goals → **+ ₹100** on Goa trip | The bar moves; Future on Home grows by ₹100; Fuel shrinks |
| 15 | Goals → **New goal**: "Laptop", 30000, 6 months, **Create goal** | A new goal card with a laptop picture |
| 16 | Home → **New payments found** | A card stack; swipe right to confirm, left to ignore; the count goes down; "All sorted" at the end |
| 17 | Tap the pet, then **Why this many days?** | An answer using Aarav's real numbers |
| 18 | In the chat, tap the language button (**English**) and choose Hindi | Answers switch to Hindi |
| 19 | Drag the pet downwards | The quick add sheet opens |
| 20 | Guide → open any book, read to the end | Pages, a 3-question quiz with feedback, takeaways, **Do it now** |
| 21 | In a book, tap an underlined word | A "Money word" card explaining it |
| 22 | Guide → **Marketplace** → **How to get it** on Spotify | The steps and an **Open Spotify** button (opens the browser or app) |
| 23 | Tap the bookmark on an offer, then **Saved** | **Saved · 1**, and only that offer |
| 24 | Profile (your initial) → **Settings** → **Night swim** | The whole app turns dark; **Daybreak** brings light back |
| 25 | Settings → Tracking | "This shared version does not read SMS…" (no SMS switch) |
| 26 | Close Ruvo fully and open it again | **Welcome back** with the PIN pad; the right PIN opens it, a wrong one is refused |
| 27 | Turn on flight mode and use the app | Everything works; only Marketplace links and Hindi book translation need internet |
| 28 | Settings → **Delete everything** (confirm) | Ruvo starts again from account creation |

## Things worth noticing

- No screen ever says "overspent" or blocks a purchase; shortfalls appear in a soft rose colour with words.
- Colours always come with a word or icon (Paid / Unpaid, bucket names), so nothing depends on colour alone.
- Amounts use Indian grouping (₹1,25,000) and dates like "3 Nov".

If anything does not match, see [Troubleshooting](TROUBLESHOOTING.md).
