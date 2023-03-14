package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.Value
import logger.issues.definition.ComputedPropertyMissingType
import logger.issues.definition.ComputedVariableWithoutSetter
import logger.issues.definition.SetterInComputedValue
import components.syntax_parser.syntax_tree.definitions.ComputedPropertyDeclaration as ComputedPropertySyntaxTree

class ComputedPropertyDeclaration(override val source: ComputedPropertySyntaxTree, scope: Scope, name: String, type: Type?,
								  isConstant: Boolean, isOverriding: Boolean, val getExpression: Value?, val setStatement: Unit?):
	InterfaceMember(source, scope, name, type, null, false, false, isConstant, false, isOverriding) {

	init {
		addUnits(getExpression, setStatement)
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): ComputedPropertyDeclaration {
		return ComputedPropertyDeclaration(source, scope, name, type?.withTypeSubstitutions(typeSubstitutions), isConstant, isOverriding,
			getExpression, setStatement)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		if(type == null)
			linter.addIssue(ComputedPropertyMissingType(source))
		if(isConstant) {
			if(setStatement != null)
				linter.addIssue(SetterInComputedValue(setStatement.source))
		} else {
			if(setStatement == null)
				linter.addIssue(ComputedVariableWithoutSetter(source))
		}
	}
}
