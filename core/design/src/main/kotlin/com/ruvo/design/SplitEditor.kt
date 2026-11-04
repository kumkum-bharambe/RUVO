package com.ruvo.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.Paise
import com.ruvo.engine.Splits

/** One friend in a split being edited: their name and, when splitting by amounts, what they owe in rupees. */
class SplitPerson(name: String = "", amount: String = "") {
    var name by mutableStateOf(name)
    var amount by mutableStateOf(amount)
}

/**
 * Splitting a payment the GPay way: add each friend, then split equally or type what each one owes. Your share
 * is always shown live, and the numbers never ask friends for more than the bill. [onChange] reports the shares
 * to save (empty means not shared).
 */
@Composable
fun SplitEditor(total: Paise, initial: List<Pair<String, Paise>>, onChange: (List<Pair<String, Paise>>) -> Unit) {
    val c = Ruvo.colors
    val people = remember { mutableStateListOf<SplitPerson>().apply { initial.forEach { add(SplitPerson(it.first, (it.second / 100).toString())) }; if (isEmpty()) add(SplitPerson()) } }
    var equally by remember { mutableStateOf(initial.isEmpty() || initial.map { it.second }.distinct().size == 1) }

    val named = people.filter { it.name.isNotBlank() }
    val owed: List<Pair<String, Paise>> = if (equally) {
        if (named.isEmpty()) emptyList() else Splits.equal("", total, named.map { it.name.trim() }).shares.map { it.person to it.amount }
    } else {
        Splits.byAmounts("", total, named.map { it.name to (parseRupees(it.amount) ?: 0) }).shares.map { it.person to it.amount }
    }
    val theirs = owed.sumOf { it.second }
    val typed = if (equally) theirs else named.sumOf { parseRupees(it.amount) ?: 0 }
    androidx.compose.runtime.SideEffect { onChange(owed) }

    SegmentedControl(listOf("Split equally", "By amount"), if (equally) 0 else 1, { equally = it == 0 })
    Spacer(Modifier.height(Space.sm))
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        people.forEachIndexed { i, p ->
            Row {
                RuvoField(p.name, { p.name = it }, "Friend ${i + 1}", Modifier.weight(1.3f))
                Spacer(Modifier.width(Space.xs))
                if (equally) {
                    val each = owed.firstOrNull { it.first == p.name.trim() }?.second ?: 0
                    Text(if (p.name.isBlank()) "" else IndianFormat.rupees(each), Modifier.weight(0.7f).align(androidx.compose.ui.Alignment.CenterVertically), style = Ruvo.type.bodyStrong, color = c.ink)
                } else {
                    RuvoField(p.amount, { p.amount = it }, "Owes", Modifier.weight(0.7f), number = true, prefix = "₹ ")
                }
            }
        }
    }
    Spacer(Modifier.height(Space.xs))
    Row {
        if (people.size < 8) GhostButton("Add a friend", { people.add(SplitPerson()) }, Modifier.weight(1f), icon = RuvoIcons.Plus)
        if (people.size > 1) {
            Spacer(Modifier.width(Space.xs))
            GhostButton("Remove last", { people.removeAt(people.lastIndex) }, Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(Space.sm))
    // The summary GPay shows: what you pay, what everyone else pays, and a warning if the amounts don't fit.
    RuvoCard(Modifier.fillMaxWidth(), brush = c.bucketBrush(com.ruvo.engine.Bucket.FIXED), border = null) {
        Row {
            Column(Modifier.weight(1f)) {
                Text("Your share", style = Ruvo.type.caption, color = c.onAccent.copy(alpha = 0.7f))
                Text(IndianFormat.rupees(total - theirs), style = Ruvo.type.title, color = c.onAccent)
            }
            Column(Modifier.weight(1f)) {
                Text("Friends pay", style = Ruvo.type.caption, color = c.onAccent.copy(alpha = 0.7f))
                Text(IndianFormat.rupees(theirs), style = Ruvo.type.title, color = c.onAccent)
            }
        }
        if (!equally && typed > total) {
            Spacer(Modifier.height(Space.xxs))
            Text("That's more than the bill of ${IndianFormat.rupees(total)}, so the last share is cut to fit.", style = Ruvo.type.caption, color = c.onAccent)
        }
    }
}
