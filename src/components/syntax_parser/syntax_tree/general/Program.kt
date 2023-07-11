package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.semantic_model.context.Context
import components.semantic_analysis.semantic_model.scopes.FileScope
import util.indent
import util.toLines
import components.semantic_analysis.semantic_model.general.Program as SemanticProgramModel

class Program(private val files: List<File>) {

    fun toSemanticModel(context: Context): SemanticProgramModel {
        val program = SemanticProgramModel(context, this)
        for(file in files)
            program.files.add(file.toSemanticModel(FileScope()))
        return program
    }

    override fun toString(): String {
        return "Program {${files.toLines().indent()}\n}"
    }
}
