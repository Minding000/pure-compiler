package linter.elements.values

import linter.elements.literals.Type
import linter.elements.general.Unit
import parsing.ast.general.Element

open class VariableValueDeclaration(open val source: Element, val name: String, var type: Type?,
									val isConstant: Boolean): Unit() {

	init {
		type?.let {
			units.add(it)
		}
	}
}