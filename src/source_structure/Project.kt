package source_structure

import code.Main
import java.lang.StringBuilder

class Project(name: String, sourceCode: String) {
    val files: Array<File>

    init {
        files = arrayOf(File(this, name, sourceCode))
    }

    override fun toString(): String {
        val string = StringBuilder()
        for (file in files)
            string.append("\n").append(file.toString())
        return "Program {${Main.indentText(string.toString())}}"
    }
}