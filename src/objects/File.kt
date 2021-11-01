package objects

import code.Main
import java.lang.StringBuilder

class File(val project: Project, val name: String, val content: String) {
    val lines: Array<Line>

    init {
        lines = content.split("\n").mapIndexed { i, rawLine -> Line(this, i + 1, rawLine) }.toTypedArray()
    }

    override fun toString(): String {
        val string = StringBuilder()
        for (line in lines)
            string.append("\n").append(line.toString())
        return "File {${Main.indentText(string.toString())}}"
    }
}