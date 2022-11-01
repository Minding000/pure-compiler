package components.parsing.syntax_tree.general

import components.linting.Linter
import components.linting.semantic_model.general.Program as SemanticProgramModel
import components.linting.semantic_model.scopes.FileScope
import util.indent
import util.toLines

class Program(private val files: List<File>) {

    fun concretize(linter: Linter): SemanticProgramModel {
        val program = SemanticProgramModel(this)
        for(file in files)
            program.files.add(file.concretize(linter, FileScope()))
        return program
    }

    override fun toString(): String {
        return "Program {${files.toLines().indent()}\n}"
    }
}
