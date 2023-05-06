package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import logger.issues.definition.ComputedVariableWithoutSetter
import logger.issues.definition.SetterInComputedValue
import components.syntax_parser.syntax_tree.definitions.ComputedPropertyDeclaration as ComputedPropertySyntaxTree

class ComputedPropertyDeclaration(override val source: ComputedPropertySyntaxTree, scope: MutableScope, name: String, type: Type?,
								  isConstant: Boolean, isOverriding: Boolean, val getExpression: Value?, val setStatement: Unit?,
								  isSpecificCopy: Boolean = false):
	PropertyDeclaration(source, scope, name, type, getExpression, false, false, isConstant, false, isOverriding,
		isSpecificCopy) {

	init {
		addUnits(getExpression, setStatement)
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): ComputedPropertyDeclaration {
		return ComputedPropertyDeclaration(source, scope, name, type?.withTypeSubstitutions(linter, typeSubstitutions), isConstant,
			isOverriding, getExpression, setStatement, true)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(isConstant) {
			if(setStatement != null)
				linter.addIssue(SetterInComputedValue(setStatement.source))
		} else {
			if(setStatement == null)
				linter.addIssue(ComputedVariableWithoutSetter(source))
		}
	}
}
