package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Value
import logger.issues.definition.ComputedVariableWithoutSetter
import logger.issues.definition.SetterInComputedValue
import components.syntax_parser.syntax_tree.definitions.ComputedPropertyDeclaration as ComputedPropertySyntaxTree

class ComputedPropertyDeclaration(override val source: ComputedPropertySyntaxTree, scope: Scope, name: String, type: Type?,
								  isConstant: Boolean, isOverriding: Boolean, val getExpression: Value?, val setStatement: Unit?):
	PropertyDeclaration(source, scope, name, type, null, false, false, isConstant, false, isOverriding) {

	init {
		addUnits(getExpression, setStatement)
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): ComputedPropertyDeclaration {
		preLinkValues(linter)
		return ComputedPropertyDeclaration(source, scope, name, type?.withTypeSubstitutions(linter, typeSubstitutions), isConstant,
			isOverriding, getExpression, setStatement)
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		if(type == null)
			type = getExpression?.type
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
