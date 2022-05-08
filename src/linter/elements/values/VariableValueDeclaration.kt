package linter.elements.values

import linter.elements.general.Unit
import linter.elements.literals.Type
import parsing.ast.general.Element

open class VariableValueDeclaration(open val source: Element, val name: String, type: Type?, val isConstant: Boolean): Unit(type)