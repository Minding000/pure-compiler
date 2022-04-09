package linter.elements.general

import parsing.ast.general.Alias

class ReferenceAlias(val source: Alias, val originalTypeName: String, val localTypeName: String): Unit()