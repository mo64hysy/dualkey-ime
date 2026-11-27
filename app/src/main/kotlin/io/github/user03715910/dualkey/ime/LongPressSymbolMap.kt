package io.github.user03715910.dualkey.ime

/**
 * Long-press symbol layer. Order: center, left, up, right, down.
 * Empty strings mean that direction is intentionally unavailable.
 */
internal object LongPressSymbolMap {
    private val values = mapOf(
        "あ" to listOf("1", "#", "!", "?", "@"),
        "か" to listOf("2", "<", "", ">", ""),
        "さ" to listOf("3", "*", "+", "/", "-"),
        "た" to listOf("4", "", "", "", ""),
        "な" to listOf("5", "{", "", "}", ""),
        "は" to listOf("6", "←", "↑", "→", "↓"),
        "ま" to listOf("7", "", "", "", ""),
        "や" to listOf("8", "(", "", ")", ""),
        "ら" to listOf("9", "", "", "", ""),
        "わ" to listOf("0", "", "", "", "")
    )

    fun forKey(key: String): List<String>? = values[key]
}
