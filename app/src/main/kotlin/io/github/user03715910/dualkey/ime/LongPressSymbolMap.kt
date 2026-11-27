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

    fun forKey(key: String, customEnabled: Boolean = false, customText: String = ""): List<String>? {
        if (!customEnabled) return values[key]
        val parsed = linkedMapOf<String, MutableList<String>>()
        customText.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.forEach { line ->
            val name = line.first().toString()
            val rest = line.drop(1)
            if (rest.isEmpty()) return@forEach
            val index = when (rest.first()) { '←' -> 1; '↑' -> 2; '→' -> 3; '↓' -> 4; else -> 0 }
            val value = if (index == 0) rest else rest.drop(1)
            if (value.isEmpty()) return@forEach
            parsed.getOrPut(name) { MutableList(5) { "" } }[index] = value
        }
        return parsed[key] ?: values[key]
    }
}
