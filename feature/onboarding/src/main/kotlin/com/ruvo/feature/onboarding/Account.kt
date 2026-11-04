package com.ruvo.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ruvo.data.Mobile
import com.ruvo.data.Pins
import com.ruvo.data.RuvoGraph
import com.ruvo.data.SignIn
import com.ruvo.data.UserSettings
import com.ruvo.design.AccentButton
import com.ruvo.design.AmbientBackdrop
import com.ruvo.design.GhostButton
import com.ruvo.design.Haptics
import com.ruvo.design.IconCircle
import com.ruvo.design.PinDots
import com.ruvo.design.PinPad
import com.ruvo.design.PrimaryButton
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoField
import com.ruvo.design.RuvoIcons
import com.ruvo.design.RuvoLogo
import com.ruvo.design.Space
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Creating a Ruvo account: your name and mobile number, then a 4-digit PIN typed twice. The account lives on
 * this phone only; Ruvo has no server, so there is no text-message code and nothing is sent anywhere.
 */
@Composable
fun CreateAccountScreen(suggestedName: String = "") {
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(suggestedName) }
    var phone by rememberSaveable { mutableStateOf("") }
    var phoneTouched by rememberSaveable { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("") }
    var shake by rememberSaveable { mutableIntStateOf(0) }
    var mismatch by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    val phoneOk = Mobile.isValid(phone)

    fun digit(d: Char) {
        if (pin.length >= Pins.LENGTH || saving) return
        pin += d
        mismatch = false
        if (pin.length < Pins.LENGTH) return
        if (step == 1) {
            first = pin; pin = ""; step = 2
        } else if (pin == first) {
            saving = true
            Haptics.confirm(view)
            scope.launch { RuvoGraph.repository.createAccount(name, phone, pin) }
        } else {
            mismatch = true; shake++; pin = ""; first = ""; step = 1
        }
    }

    AuthFrame(onBack = if (step > 0 && !saving) ({ pin = ""; mismatch = false; step -= 1 }) else null) {
        AnimatedContent(
            step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally { w -> if (forward) w / 4 else -w / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { w -> if (forward) -w / 4 else w / 4 } + fadeOut())
            },
            label = "account",
        ) { s ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (s == 0) {
                    Title("Create your Ruvo account", "It stays on this phone. Ruvo has no server, so nothing about you is sent anywhere.")
                    Spacer(Modifier.height(Space.lg))
                    RuvoField(name, { name = it }, "Your name")
                    Spacer(Modifier.height(Space.sm))
                    RuvoField(
                        phone, { phone = it; if (it.length >= 10) phoneTouched = true }, "Mobile number", prefix = "+91 ", phone = true,
                        error = if (phoneTouched && !phoneOk) "Enter a 10-digit number starting with 6, 7, 8 or 9." else null,
                    )
                    Spacer(Modifier.height(Space.lg))
                    PrimaryButton("Continue", { phoneTouched = true; if (phoneOk && name.isNotBlank()) step = 1 }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && phone.length == 10)
                    Spacer(Modifier.height(Space.md))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(RuvoIcons.Shield, null, Modifier.size(18.dp), tint = c.inkFaint)
                        Spacer(Modifier.width(Space.xs))
                        Text("Your number is only used to recognise your account on this phone. Ruvo never calls or texts it.", style = Ruvo.type.caption, color = c.inkFaint)
                    }
                } else {
                    Title(
                        if (s == 1) "Choose a 4-digit PIN" else "Type it again",
                        if (s == 1) "You'll use it each time you open Ruvo." else "Just to be sure it's the one you meant.",
                    )
                    Spacer(Modifier.height(Space.xl))
                    PinDots(pin.length, shake, error = mismatch)
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        if (mismatch) "Those didn't match. Choose your PIN again." else " ",
                        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = Ruvo.type.caption, color = c.warning,
                    )
                    Spacer(Modifier.height(Space.lg))
                    PinPad({ digit(it) }, { pin = pin.dropLast(1) }, enabled = !saving)
                }
            }
        }
    }
}

/**
 * Signing back in: "Welcome back", the masked number, and the PIN pad. Five wrong PINs pause tries for 30
 * seconds. A forgotten PIN can't be recovered (it isn't stored anywhere), so the only way on is to reset Ruvo
 * on this phone, which asks first.
 */
@Composable
fun SignInScreen(settings: UserSettings) {
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var pin by remember { mutableStateOf("") }
    var shake by rememberSaveable { mutableIntStateOf(0) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var checking by rememberSaveable { mutableStateOf(false) }
    var forgot by rememberSaveable { mutableStateOf(false) }
    var pausedUntil by rememberSaveable { mutableLongStateOf(settings.pinPausedUntil) }
    var now by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    val paused = pausedUntil > now
    LaunchedEffect(pausedUntil) {
        while (pausedUntil > System.currentTimeMillis()) { now = System.currentTimeMillis(); delay(500) }
        now = System.currentTimeMillis()
        if (pausedUntil != 0L && message?.startsWith("Too many") == true) message = null
    }

    fun digit(d: Char) {
        if (pin.length >= Pins.LENGTH || checking || paused) return
        pin += d
        if (pin.length < Pins.LENGTH) return
        checking = true
        scope.launch {
            when (val r = RuvoGraph.repository.signIn(pin)) {
                SignIn.Ok -> Haptics.confirm(view)
                is SignIn.Wrong -> { message = "Wrong PIN. ${r.triesLeft} ${if (r.triesLeft == 1) "try" else "tries"} left."; shake++ }
                is SignIn.Paused -> { pausedUntil = r.untilMillis; now = System.currentTimeMillis(); message = "Too many tries."; shake++ }
            }
            pin = ""
            checking = false
        }
    }

    val first = settings.accountName.substringBefore(' ').ifBlank { "there" }
    AuthFrame(onBack = null) {
        Title("Welcome back, $first", "+91 ${Mobile.masked(settings.accountPhone)}")
        Spacer(Modifier.height(Space.xl))
        PinDots(pin.length, shake, error = message != null && pin.isEmpty())
        Spacer(Modifier.height(Space.sm))
        val secondsLeft = ((pausedUntil - now + 999) / 1000).coerceAtLeast(0)
        Text(
            when {
                paused -> "Too many tries. Try again in $secondsLeft s."
                message != null -> message!!
                else -> "Enter your PIN"
            },
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = Ruvo.type.caption, color = if (paused || message != null) c.warning else c.inkSoft,
        )
        Spacer(Modifier.height(Space.lg))
        PinPad({ digit(it) }, { pin = pin.dropLast(1) }, enabled = !paused && !checking)
        Spacer(Modifier.height(Space.md))
        if (!forgot) {
            TextButton({ forgot = true }, Modifier.heightIn(min = 48.dp)) { Text("Forgot PIN?", style = Ruvo.type.label, color = c.ink) }
        } else {
            RuvoCard(Modifier.fillMaxWidth(), color = c.warning.copy(alpha = 0.1f), border = c.warning.copy(alpha = 0.5f)) {
                Text("Reset Ruvo on this phone?", style = Ruvo.type.title, color = c.ink)
                Text(
                    "Your PIN isn't stored anywhere, so it can't be sent to you. Resetting deletes your account and everything in Ruvo on this phone. Your bank account and SMS are not touched.",
                    style = Ruvo.type.body, color = c.inkSoft,
                )
                Spacer(Modifier.height(Space.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    GhostButton("Keep trying", { forgot = false }, Modifier.weight(1f))
                    AccentButton("Reset Ruvo", { scope.launch { RuvoGraph.repository.deleteEverything() } }, Modifier.weight(1f), accent = c.warning)
                }
            }
        }
    }
}

@Composable
private fun Title(title: String, line: String) {
    val c = Ruvo.colors
    Text(title, Modifier.fillMaxWidth().semantics { heading() }, style = Ruvo.type.headline, color = c.ink, textAlign = TextAlign.Center)
    Spacer(Modifier.height(Space.xs))
    Text(line, Modifier.fillMaxWidth(), style = Ruvo.type.body, color = c.inkSoft, textAlign = TextAlign.Center)
}

/** The shared frame: the backdrop, an optional back button, the mark, and content that scrolls on small phones. */
@Composable
private fun AuthFrame(onBack: (() -> Unit)?, content: @Composable () -> Unit) {
    AmbientBackdrop {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter, vertical = Space.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                if (onBack != null) IconCircle(RuvoIcons.Back, "Back", onBack)
            }
            Spacer(Modifier.height(Space.md))
            RuvoLogo(size = 64.dp)
            Spacer(Modifier.height(Space.lg))
            content()
        }
    }
}

/** Profile → Change PIN: the current PIN, then the new one twice. Nothing changes until all three are right. */
@Composable
fun ChangePinSheet(onDismiss: () -> Unit) {
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var step by remember { mutableIntStateOf(0) }
    var pin by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var shake by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }

    fun digit(d: Char) {
        if (pin.length >= Pins.LENGTH || done) return
        pin += d
        message = null
        if (pin.length < Pins.LENGTH) return
        when (step) {
            0 -> { current = pin; pin = ""; step = 1 }
            1 -> { next = pin; pin = ""; step = 2 }
            else -> if (pin != next) {
                message = "Those didn't match. Choose the new PIN again."; shake++; pin = ""; next = ""; step = 1
            } else scope.launch {
                if (RuvoGraph.repository.changePin(current, next)) { done = true; Haptics.confirm(view) }
                else { message = "Your current PIN wasn't right. Start again."; shake++; step = 0 }
                pin = ""; current = ""; next = ""
            }
        }
    }

    com.ruvo.design.RuvoSheet("Change PIN", onDismiss, eyebrow = "Account") {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (done) {
                Text("PIN changed. Use the new one next time you open Ruvo.", style = Ruvo.type.body, color = c.ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Space.md))
                PrimaryButton("Done", onDismiss, Modifier.fillMaxWidth())
            } else {
                Text(
                    when (step) { 0 -> "Enter your current PIN"; 1 -> "Choose a new 4-digit PIN"; else -> "Type the new PIN again" },
                    style = Ruvo.type.title, color = c.ink,
                )
                Spacer(Modifier.height(Space.lg))
                PinDots(pin.length, shake, error = message != null && pin.isEmpty())
                Spacer(Modifier.height(Space.sm))
                Text(message ?: " ", Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = Ruvo.type.caption, color = c.warning, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Space.md))
                PinPad({ digit(it) }, { pin = pin.dropLast(1) })
            }
        }
    }
}
