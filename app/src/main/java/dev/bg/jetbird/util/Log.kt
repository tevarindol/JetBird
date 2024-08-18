package dev.bg.jetbird.util

object Log {
    fun getLogcatLineContent(line: String): String {
        val s = line.split("GoLog")
        if (s.size != 2) return line
        return "[${s.first().split(" ")[1]}] ${s.last().drop(5)}"
    }
}
