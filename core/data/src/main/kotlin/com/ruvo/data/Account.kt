package com.ruvo.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Ruvo's account lives on the phone only: a name, a mobile number and a 4-digit PIN. There is no server, so
 * nothing is sent anywhere and there is no code by SMS. The PIN is never stored, only a salted PBKDF2 hash.
 */
object Pins {
    const val LENGTH = 4
    private const val ITERATIONS = 60_000
    private const val BITS = 256

    fun isValid(pin: String) = pin.length == LENGTH && pin.all { it.isDigit() }

    /** "v1:salt:hash", both Base64. A new random salt every time. */
    fun hash(pin: String, salt: ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }): String {
        val enc = Base64.getEncoder()
        return "v1:${enc.encodeToString(salt)}:${enc.encodeToString(derive(pin, salt))}"
    }

    fun matches(pin: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 3 || parts[0] != "v1") return false
        val dec = Base64.getDecoder()
        val salt = runCatching { dec.decode(parts[1]) }.getOrNull() ?: return false
        val want = runCatching { dec.decode(parts[2]) }.getOrNull() ?: return false
        return MessageDigest.isEqual(derive(pin, salt), want)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}

/** A 10-digit Indian mobile number starting 6 to 9, with spaces and a +91 or 0 prefix ignored. */
object Mobile {
    fun normalise(raw: String): String {
        val d = raw.filter { it.isDigit() }
        return when {
            d.length == 12 && d.startsWith("91") -> d.drop(2)
            d.length == 11 && d.startsWith("0") -> d.drop(1)
            else -> d
        }
    }

    fun isValid(raw: String): Boolean = normalise(raw).let { it.length == 10 && it.first() in '6'..'9' }

    /** "98•••••210": enough to recognise it, not enough to read it off the screen. */
    fun masked(n: String): String = if (n.length == 10) n.take(2) + "•".repeat(5) + n.takeLast(3) else n
}

/** Whether this run of the app has been signed in to. Kept in memory only, so every cold start asks again. */
object Session {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()
    fun unlock() { _unlocked.value = true }
    fun lock() { _unlocked.value = false }
}

/** What happened when a PIN was tried. */
sealed interface SignIn {
    data object Ok : SignIn
    data class Wrong(val triesLeft: Int) : SignIn
    data class Paused(val untilMillis: Long) : SignIn
}
