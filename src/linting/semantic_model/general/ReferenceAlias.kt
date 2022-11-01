package linting.semantic_model.general

import components.parsing.syntax_tree.general.ReferenceAlias

class ReferenceAlias(override val source: ReferenceAlias, val originalTypeName: String, val localTypeName: String):
	Unit(source)
