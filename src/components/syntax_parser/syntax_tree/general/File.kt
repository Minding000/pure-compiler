package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.File as SemanticFileModel
import source_structure.File as SourceFile
import components.semantic_analysis.semantic_model.scopes.FileScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import source_structure.Position
import util.indent
import util.toLines

class File(start: Position, end: Position, private val file: SourceFile, private val statements: List<Element>):
	Element(start, end) {

    override fun concretize(linter: Linter, scope: MutableScope): SemanticFileModel {
        val file = SemanticFileModel(this, file, scope as FileScope)
        for(statement in statements)
            statement.concretize(linter, scope, file.units)
        return file
    }

    override fun toString(): String {
        return "File {${statements.toLines().indent()}\n}"
    }
}
