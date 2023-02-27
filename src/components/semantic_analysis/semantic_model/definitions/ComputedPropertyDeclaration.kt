package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.Value
import messages.Message
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
			linter.addMessage(source, "Computed properties need to have an explicitly declared type.",
				Message.Type.ERROR)
		if(isConstant) {
			if(setStatement != null)
				linter.addMessage(setStatement.source, "Computed value property cannot have a setter.",
					Message.Type.ERROR)
		} else {
			if(setStatement == null)
				linter.addMessage(source, "Computed variable property needs to have a setter.",
					Message.Type.ERROR)
		}
	}
}
