package linter.elements.values

import linter.elements.general.Unit
import parsing.ast.general.Element

open class VariableValueDeclaration(open val source: Element, val name: String, val isMutable: Boolean = false): Unit()