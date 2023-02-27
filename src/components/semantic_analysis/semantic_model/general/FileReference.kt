package components.semantic_analysis.semantic_model.general

import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.FileReference as FileReferenceSyntaxTree

class FileReference(override val source: FileReferenceSyntaxTree, scope: Scope, val identifier: String, val parts: List<String>,
					val aliases: List<ReferenceAlias>): Unit(source, scope) {

	init { //TODO write test
		addUnits(aliases)
	}
}
