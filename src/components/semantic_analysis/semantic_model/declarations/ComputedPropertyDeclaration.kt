package components.semantic_analysis.semantic_model.declarations

import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import logger.issues.declaration.ComputedVariableWithoutSetter
import logger.issues.declaration.SetterInComputedValue
import components.syntax_parser.syntax_tree.definitions.ComputedPropertyDeclaration as ComputedPropertySyntaxTree

class ComputedPropertyDeclaration(override val source: ComputedPropertySyntaxTree, scope: MutableScope, name: String, type: Type?,
								  isConstant: Boolean, isOverriding: Boolean, val getExpression: Value?, val setStatement: SemanticModel?,
								  isSpecificCopy: Boolean = false):
	PropertyDeclaration(source, scope, name, type, getExpression, false, false, isConstant, false, isOverriding,
		isSpecificCopy) {

	init {
		addSemanticModels(setStatement)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): ComputedPropertyDeclaration {
		return ComputedPropertyDeclaration(source, scope, name, type?.withTypeSubstitutions(typeSubstitutions), isConstant,
			isOverriding, getExpression, setStatement, true)
	}

	override fun validate() {
		super.validate()
		if(isConstant) {
			if(setStatement != null)
				context.addIssue(SetterInComputedValue(setStatement.source))
		} else {
			if(setStatement == null)
				context.addIssue(ComputedVariableWithoutSetter(source))
		}
	}
}
