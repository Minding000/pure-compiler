package parsing.ast.general

import linter.Linter
import linter.elements.general.Program
import linter.scopes.FileScope
import util.indent
import util.toLines

class Program(private val files: List<File>) {

    fun concretize(linter: Linter): Program {
        val program = Program(this)
        for(file in files)
            program.files.add(file.concretize(linter, FileScope()))
        return program
    }

    override fun toString(): String {
        return "Program {${files.toLines().indent()}\n}"
    }
}