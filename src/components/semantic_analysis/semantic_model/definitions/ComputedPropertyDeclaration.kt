package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.MemberDeclaration
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.definitions.ComputedPropertyDeclaration as ComputedPropertySyntaxTree

class ComputedPropertyDeclaration(override val source: ComputedPropertySyntaxTree, name: String, type: Type?,
								  val getExpression: Value?, val setExpression: Value?):
	MemberDeclaration(source, name, type, null, setExpression == null) {

	init {
		addUnits(getExpression, setExpression)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): ComputedPropertyDeclaration {
		return ComputedPropertyDeclaration(source, name, type?.withTypeSubstitutions(typeSubstitutions), getExpression,
			setExpression)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		//TODO validate that a type has been provided (if not already validated somewhere else)
	}
}
