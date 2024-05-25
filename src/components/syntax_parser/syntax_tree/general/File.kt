package components.syntax_parser.syntax_tree.general

import components.semantic_model.scopes.FileScope
import components.semantic_model.scopes.MutableScope
import util.indent
import util.toLines
import util.toSemanticModels
import components.semantic_model.general.File as SemanticFileModel
import source_structure.File as SourceFile

class File(private val file: SourceFile, private val statements: List<SyntaxTreeNode>): SyntaxTreeNode(file.getStart(), file.getEnd()) {

	override fun toSemanticModel(scope: MutableScope): SemanticFileModel {
		return SemanticFileModel(this, file, scope as FileScope, statements.toSemanticModels(scope))
	}

	override fun toString(): String {
		return "File {${statements.toLines().indent()}\n}"
	}
}
