package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.values.VariableValueDeclaration
import parsing.ast.definitions.ComputedProperty

class ComputedProperty(override val source: ComputedProperty, name: String, val type: Unit, val getExpression: Unit?, val setExpression: Unit?):
	VariableValueDeclaration(source, name, setExpression == null) {

	init {
		units.add(type)
		if(getExpression != null)
			units.add(getExpression)
		if(setExpression != null)
			units.add(setExpression)
	}
}