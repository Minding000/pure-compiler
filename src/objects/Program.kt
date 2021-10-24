package objects

import code.Main
import java.lang.StringBuilder

class Program(sourceCode: String) {
    val files: Array<File>

    init {
        files = arrayOf(File(this, sourceCode))
    }

    override fun toString(): String {
        val string = StringBuilder()
        for (file in files)
            string.append("\n").append(file.toString())
        return "Program {${Main.indentText(string.toString())}}"
    }
}