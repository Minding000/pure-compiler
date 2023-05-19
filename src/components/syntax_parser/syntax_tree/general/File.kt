package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.semantic_model.scopes.FileScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import util.concretize
import util.indent
import util.toLines
import components.semantic_analysis.semantic_model.general.File as SemanticFileModel
import source_structure.File as SourceFile

class File(private val file: SourceFile, private val statements: List<Element>): Element(file.getStart(), file.getEnd()) {

    override fun concretize(scope: MutableScope): SemanticFileModel {
        return SemanticFileModel(this, file, scope as FileScope, statements.concretize(scope))
    }

    override fun toString(): String {
        return "File {${statements.toLines().indent()}\n}"
    }
}
