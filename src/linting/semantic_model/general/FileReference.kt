package linting.semantic_model.general

import components.parsing.syntax_tree.general.FileReference

class FileReference(override val source: FileReference, val identifier: String, val parts: List<String>,
					val aliases: List<ReferenceAlias>): Unit(source) {

	init {
		units.addAll(aliases)
	}
}
