package components.semantic_model.declarations

import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.Type
import components.semantic_model.values.Value
import logger.issues.declaration.ComputedVariableWithoutSetter
import logger.issues.declaration.SetterInComputedValue
import components.syntax_parser.syntax_tree.definitions.ComputedPropertyDeclaration as ComputedPropertySyntaxTree

class ComputedPropertyDeclaration(override val source: ComputedPropertySyntaxTree, scope: MutableScope, name: String, type: Type?,
								  isConstant: Boolean, isOverriding: Boolean, val getExpression: Value?, val setStatement: SemanticModel?):
	PropertyDeclaration(source, scope, name, type, getExpression, false, false, isConstant, false, isOverriding) {

	init {
		addSemanticModels(setStatement)
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
