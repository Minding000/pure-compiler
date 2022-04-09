package parsing.ast.general

import linter.Linter
import linter.elements.general.FileReference
import linter.elements.general.ReferenceAlias
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.literals.Identifier
import source_structure.Position
import util.indent
import util.toLines
import java.lang.StringBuilder
import java.util.*

class FileReference(start: Position, private val parts: List<Identifier>, private val body: AliasBlock?): Element(start, body?.end ?: parts.last().end) {

	override fun concretize(linter: Linter, scope: Scope): FileReference {
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