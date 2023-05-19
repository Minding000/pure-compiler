package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.semantic_model.scopes.FileScope
import util.indent
import util.toLines
import components.semantic_analysis.semantic_model.general.Program as SemanticProgramModel

class Program(private val files: List<File>) {

    fun concretize(): SemanticProgramModel {
        val program = SemanticProgramModel(this)
        for(file in files)
            program.files.add(file.concretize(FileScope()))
        return program
    }

    override fun toString(): String {
        return "Program {${files.toLines().indent()}\n}"
    }
}
