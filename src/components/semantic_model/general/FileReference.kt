package components.semantic_model.general

import components.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.FileReference as FileReferenceSyntaxTree

class FileReference(override val source: FileReferenceSyntaxTree, scope: Scope, val identifier: String, val parts: List<String>,
					val aliases: List<ReferenceAlias>): SemanticModel(source, scope) {

	init {
		addSemanticModels(aliases)
	}

	fun getNameAliases(): Map<String, String> {
		val nameAliases = HashMap<String, String>()
		for(alias in aliases)
			nameAliases[alias.originalName] = alias.localName
		return nameAliases
	}
}
