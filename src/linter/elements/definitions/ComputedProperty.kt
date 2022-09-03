package linter.elements.definitions

import linter.elements.general.Unit
import linter.elements.literals.Type
import linter.elements.values.VariableValueDeclaration
import parsing.ast.definitions.ComputedProperty

class ComputedProperty(override val source: ComputedProperty, name: String, type: Type, val getExpression: Unit?, val setExpression: Unit?):
	VariableValueDeclaration(source, name, type, null, setExpression == null) {

	init {
		units.add(type)
		if(getExpression != null)
			units.add(getExpression)
		if(setExpression != null)
			units.add(setExpression)
	}
}