package parsing.syntax_tree.general

import linting.Linter
import linting.semantic_model.general.FileReference
import linting.semantic_model.general.ReferenceAlias
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.literals.Identifier
import source_structure.Position
import util.indent
import util.toLines
import java.util.*

class FileReference(start: Position, private val parts: List<Identifier>, private val body: AliasBlock?): Element(start, body?.end ?: parts.last().end) {

	override fun concretize(linter: Linter, scope: MutableScope): FileReference {
		val parts = LinkedList<String>()
		for(part in this.parts)
			parts.add(part.getValue())
		val aliases = LinkedList<ReferenceAlias>()
		if(this.body != null) {
			for(alias in this.body.aliases)
				aliases.add(alias.concretize(linter, scope))
		}
		return FileReference(this, parts.joinToString("."), parts, aliases)
	}

	override fun toString(): String {
		return "FileReference {${"${parts.toLines()}${if(body != null) "\n$body" else ""}".indent()}\n}"
	}
}