package io.github.user03715910.dualkey.ime

/** English key flick map. Order: center, left, up, right, down. */
internal object EnglishFlickMap {
    private val defaults = buildMap<Char, List<String>> {
        val top = "qwertyuiop"
        val topLeft = "☆♪→○×÷<>^|"
        val topUp = "1234567890"
        top.forEachIndexed { i, c -> put(c, listOf(c.toString(), topLeft[i].toString(), topUp[i].toString(), "", c.uppercase())) }
        val middle = "@#¥$%&_-+"
        "asdfghjkl".forEachIndexed { i, c ->
            val left = if (c in "fghjkl") "()[]\\/"["fghjkl".indexOf(c)].toString() else ""
            put(c, listOf(c.toString(), left, middle[i].toString(), "", c.uppercase()))
        }
        val bottom = "\"*'/!?~"
        "zxcvbnm".forEachIndexed { i, c -> put(c, listOf(c.toString(), "", bottom[i].toString(), "", c.uppercase())) }
    }

    fun forKey(key: Char, customEnabled: Boolean = false, customText: String = ""): List<String> {
        if (!customEnabled) return defaults[key] ?: listOf(key.toString(), "", "", "", key.uppercase())
        val parsed = linkedMapOf<Char, MutableList<String>>()
        customText.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.forEach { line ->
            val arrow = line.indexOfFirst { it in "←↑→↓" }
            if (arrow <= 0 || arrow == line.lastIndex) return@forEach
            val letters = line.substring(0, arrow)
            val direction = when (line[arrow]) { '←' -> 1; '↑' -> 2; '→' -> 3; else -> 4 }
            val chars = line.substring(arrow + 1)
            if (letters.length != chars.length) return@forEach
            letters.forEachIndexed { i, letter ->
                val list = parsed.getOrPut(letter) { MutableList(5) { "" } }
                list[direction] = chars[i].toString()
            }
        }
        return parsed[key]?.toList() ?: defaults[key] ?: listOf(key.toString(), "", "", "", key.uppercase())
    }
}
