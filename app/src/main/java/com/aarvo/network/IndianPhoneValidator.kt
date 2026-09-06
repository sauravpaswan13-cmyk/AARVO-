package com.aarvo.network

/** Production validation for Indian mobile numbers.
 *
 * Accepts exactly 10 ASCII digits and requires the first digit to be 6-9.
 * Country code (+91 / 91) is intentionally handled by the UI/API boundary,
 * so this validator has one canonical representation: 10 digits.
 */
object IndianPhoneValidator {
    private val pattern = Regex("^[6-9][0-9]{9}$")

    fun isValid(value: String): Boolean = pattern.matches(value.trim())

    fun normalize(value: String): String {
        val compact = value.trim().replace("\\s".toRegex(), "")
        val withoutCountryCode = when {
            compact.startsWith("+91") -> compact.removePrefix("+91")
            compact.startsWith("91") && compact.length == 12 -> compact.removePrefix("91")
            else -> compact
        }
        return withoutCountryCode
    }

    fun isValidOrThrow(value: String): String {
        val normalized = normalize(value)
        require(isValid(normalized)) {
            "Enter a valid 10-digit Indian mobile number starting with 6, 7, 8 or 9."
        }
        return normalized
    }
}
