package com.footballxtream.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a real device because [Crypto] uses the Android Keystore (no JVM/Robolectric equivalent).
 * Covers the encryption round-trip and the legacy-plaintext fallback that the credential storage
 * relies on.
 */
@RunWith(AndroidJUnit4::class)
class CryptoInstrumentedTest {

    @Test
    fun roundTrip_recoversTheOriginal() {
        val secret = "user1234:passw0rd!"
        val enc = Crypto.encrypt(secret)

        assertTrue("encrypted value is tagged", enc.startsWith("enc:"))
        assertNotEquals("ciphertext is not the plaintext", secret, enc)
        assertEquals(secret, Crypto.decrypt(enc))
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals("", Crypto.encrypt(""))
        assertEquals("", Crypto.decrypt(""))
    }

    @Test
    fun legacyPlaintext_passesThroughOnDecrypt() {
        // Values written before encryption existed have no "enc:" tag → returned unchanged.
        assertEquals("legacy-plain-value", Crypto.decrypt("legacy-plain-value"))
    }

    @Test
    fun encryptionIsRandomized_butBothDecrypt() {
        val secret = "same-input"
        val a = Crypto.encrypt(secret)
        val b = Crypto.encrypt(secret)

        assertNotEquals("a fresh IV makes each ciphertext different", a, b)
        assertEquals(secret, Crypto.decrypt(a))
        assertEquals(secret, Crypto.decrypt(b))
    }

    @Test
    fun tamperedOrInvalidCiphertext_returnsEmpty() {
        assertEquals("", Crypto.decrypt("enc:not-valid-base64-or-gcm"))
    }
}
