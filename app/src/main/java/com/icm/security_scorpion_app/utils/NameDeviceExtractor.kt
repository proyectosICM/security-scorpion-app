package com.icm.security_scorpion_app.utils


import java.util.regex.Pattern

class NameDeviceExtractor {
    companion object {
        fun extractName(message: String?): String? {
            val pattern = Pattern.compile("""nombre: ([^:]+)""")
            val matcher = pattern.matcher(message ?: "")
            return if (matcher.find()) matcher.group(1) else null
        }

        fun extractId(message: String?): String? {
            val pattern = Pattern.compile("""id: (\d+)""")
            val matcher = pattern.matcher(message ?: "")
            return if (matcher.find()) matcher.group(1) else null
        }
    }
}
