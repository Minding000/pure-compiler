package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.ReferenceAlias
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position
import util.indent
import util.toLines
import java.util.*
import components.semantic_analysis.semantic_model.general.FileReference as SemanticFileReferenceModel

class FileReference(start: Position, private val parts: List<Identifier>, private val body: AliasBlock?):
	Element(start, body?.end ?: parts.last().end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticFileReferenceModel {
		val parts = LinkedList<String>()
		for(part in this.parts)
			parts.add(part.getValue())
		val aliases = LinkedList<ReferenceAlias>()
		if(this.body != null) {
			for(alias in this.body.referenceAliases)
				aliases.add(alias.concretize(linter, scope))
		}
		return SemanticFileReferenceModel(this, scope, parts.joinToString("."), parts, aliases)
	}

	override fun toString(): String {
		return "FileReference {${"${parts.toLines()}${if(body != null) "\n$body" else ""}".indent()}\n}"
	}
}
