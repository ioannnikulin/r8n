package com.r8n.backend.users.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserAgentOperatingSystemParserTest {
    private val parser = UserAgentOperatingSystemParser()

    @Test
    fun `parses common operating systems from user agents`() {
        assertEquals(
            "Windows",
            parser.parse("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"),
        )
        assertEquals(
            "macOS",
            parser.parse("Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4_1) AppleWebKit/605.1.15"),
        )
        assertEquals(
            "iOS",
            parser.parse("Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15"),
        )
        assertEquals(
            "Android",
            parser.parse("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36"),
        )
        assertEquals(
            "Linux",
            parser.parse("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36"),
        )
    }

    @Test
    fun `returns unknown for missing or unrecognized user agents`() {
        assertEquals("Unknown", parser.parse(null))
        assertEquals("Unknown", parser.parse("   "))
        assertEquals("Unknown", parser.parse("Login Test Agent"))
    }
}
