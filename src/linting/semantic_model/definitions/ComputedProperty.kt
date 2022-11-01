package linting.semantic_model.definitions

import linting.semantic_model.types.Type
import linting.semantic_model.values.Value
import linting.semantic_model.values.VariableValueDeclaration
import components.parsing.syntax_tree.definitions.ComputedProperty

class ComputedProperty(override val source: ComputedProperty, name: String, type: Type, val getExpression: Value?,
					   val setExpression: Value?):
	VariableValueDeclaration(source, name, type, null, setExpression == null) {

	init {
		if(getExpression != null)
			units.add(getExpression)
		if(setExpression != null)
			units.add(setExpression)
	}
}
