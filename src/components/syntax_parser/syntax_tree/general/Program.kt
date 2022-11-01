package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Program as SemanticProgramModel
import components.semantic_analysis.semantic_model.scopes.FileScope
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
