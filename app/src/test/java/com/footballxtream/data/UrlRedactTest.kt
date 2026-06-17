package com.footballxtream.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UrlRedactTest {

    @Test
    fun stripsM3uQueryCredentials() {
        val redacted = redactUrl("http://host.example.com:8080/get.php?username=secretuser&password=secretpass&type=m3u_plus")

        assertEquals("http://host.example.com:8080/…", redacted)
        assertFalse(redacted.contains("secretuser"))
        assertFalse(redacted.contains("secretpass"))
    }

    @Test
    fun stripsUserInfoCredentials() {
        val redacted = redactUrl("http://user:pass@host.example.com/playlist.m3u")

        assertEquals("http://host.example.com/…", redacted)
        assertFalse(redacted.contains("user:pass"))
    }

    @Test
    fun keepsHostForHttpsWithoutPort() {
        assertEquals("https://epg.example.com/…", redactUrl("https://epg.example.com/guide.xml.gz"))
    }

    @Test
    fun malformedUrlNeverLeaksTheInput() {
        // A string with credentials but no parseable host must not echo the credentials back.
        val redacted = redactUrl("username=u&password=p")
        assertFalse(redacted.contains("password=p"))
    }
}
