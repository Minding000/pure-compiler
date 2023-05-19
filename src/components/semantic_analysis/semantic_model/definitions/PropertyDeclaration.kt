package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.modifiers.OverridingPropertyTypeMismatch
import logger.issues.modifiers.OverridingPropertyTypeNotAssignable
import logger.issues.modifiers.VariablePropertyOverriddenByValue

open class PropertyDeclaration(source: Element, scope: MutableScope, name: String, type: Type? = null, value: Value? = null,
							   isStatic: Boolean = false, isAbstract: Boolean = false, isConstant: Boolean = true,
							   isMutable: Boolean = false, isOverriding: Boolean = false, isSpecificCopy: Boolean = false):
	InterfaceMember(source, scope, name, type, value, isStatic, isAbstract, isConstant, isMutable, isOverriding, isSpecificCopy) {

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): PropertyDeclaration {
		return PropertyDeclaration(source, scope, name, type?.withTypeSubstitutions(typeSubstitutions), value, isStatic, isAbstract,
			isConstant, isMutable, isOverriding, true)
	}

	override fun determineTypes() {
		if(!context.declarationStack.push(this))
			return
		super.determineTypes()
		context.declarationStack.pop(this)
	}

	override fun validate() {
		super.validate()
		validateSuperMember()
	}

	private fun validateSuperMember() {
		val superMember = superMember ?: return
		if(!superMember.isConstant && isConstant)
			context.addIssue(VariablePropertyOverriddenByValue(this))
		val type = type ?: return
		val superType = superMember.type ?: return
		if(type is FunctionType && superType is FunctionType)
			return
		if(isConstant) {
			if(!type.isAssignableTo(superType))
				context.addIssue(OverridingPropertyTypeNotAssignable(type, superType))
		} else {
			if(type != superType)
				context.addIssue(OverridingPropertyTypeMismatch(type, superType))
		}
	}
}
