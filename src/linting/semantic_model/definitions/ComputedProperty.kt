package linting.semantic_model.definitions

import linting.semantic_model.general.Unit
import linting.semantic_model.types.Type
import linting.semantic_model.values.VariableValueDeclaration
import parsing.syntax_tree.definitions.ComputedProperty

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
