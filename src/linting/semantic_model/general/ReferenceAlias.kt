package linting.semantic_model.general

import parsing.syntax_tree.general.Alias

class ReferenceAlias(val source: Alias, val originalTypeName: String, val localTypeName: String): Unit()