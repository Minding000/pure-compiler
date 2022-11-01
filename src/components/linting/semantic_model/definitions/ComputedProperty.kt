package components.linting.semantic_model.definitions

import components.linting.semantic_model.types.Type
import components.linting.semantic_model.values.Value
import components.linting.semantic_model.values.VariableValueDeclaration
import components.syntax_parser.syntax_tree.definitions.ComputedProperty as ComputedPropertySyntaxTree

class ComputedProperty(override val source: ComputedPropertySyntaxTree, name: String, type: Type,
					   val getExpression: Value?, val setExpression: Value?):
	VariableValueDeclaration(source, name, type, null, setExpression == null) {

	init {
		if(getExpression != null)
			units.add(getExpression)
		if(setExpression != null)
			units.add(setExpression)
	}
}
