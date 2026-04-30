package com.r8n.backend.users.service

import org.springframework.stereotype.Component
import java.util.Locale

@Component
class UserAgentOperatingSystemParser {
    fun parse(userAgent: String?): String {
        val normalized = userAgent?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: return UNKNOWN

        return when {
            normalized.contains("windows nt") -> "Windows"
            normalized.contains("android") -> "Android"
            normalized.contains("iphone") || normalized.contains("ipad") || normalized.contains("ipod") -> "iOS"
            normalized.contains("mac os x") || normalized.contains("macintosh") -> "macOS"
            normalized.contains("cros") -> "ChromeOS"
            normalized.contains("linux") || normalized.contains("x11") -> "Linux"
            else -> UNKNOWN
        }
    }

    private companion object {
        const val UNKNOWN = "Unknown"
    }
}
