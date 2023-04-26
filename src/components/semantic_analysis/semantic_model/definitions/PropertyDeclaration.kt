package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.initialization.CircularAssignment
import logger.issues.modifiers.OverridingPropertyTypeMismatch
import logger.issues.modifiers.OverridingPropertyTypeNotAssignable
import logger.issues.modifiers.VariablePropertyOverriddenByValue

open class PropertyDeclaration(source: Element, scope: MutableScope, name: String, type: Type? = null, value: Value? = null,
							   isStatic: Boolean = false, isAbstract: Boolean = false, isConstant: Boolean = true,
							   isMutable: Boolean = false, isOverriding: Boolean = false, isSpecificCopy: Boolean = false):
	InterfaceMember(source, scope, name, type, value, isStatic, isAbstract, isConstant, isMutable, isOverriding, isSpecificCopy) {

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): PropertyDeclaration {
		return PropertyDeclaration(source, scope, name, type?.withTypeSubstitutions(linter, typeSubstitutions), value, isStatic, isAbstract,
			isConstant, isMutable, isOverriding, true)
	}

	override fun linkValues(linter: Linter) {
		if(linter.propertyDeclarationStack.contains(this)) {
			for(propertyDeclaration in linter.propertyDeclarationStack)
				linter.addIssue(CircularAssignment(propertyDeclaration))
			return
		}
		linter.propertyDeclarationStack.add(this)
		super.linkValues(linter)
		linter.propertyDeclarationStack.remove(this)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		validateSuperMember(linter)
	}

	private fun validateSuperMember(linter: Linter) {
		val superMember = superMember ?: return
		if(!superMember.isConstant && isConstant)
			linter.addIssue(VariablePropertyOverriddenByValue(this))
		val type = type ?: return
		val superType = superMember.type ?: return
		if(type is FunctionType && superType is FunctionType)
			return
		if(isConstant) {
			if(!type.isAssignableTo(superType))
				linter.addIssue(OverridingPropertyTypeNotAssignable(type, superType))
		} else {
			if(type != superType)
				linter.addIssue(OverridingPropertyTypeMismatch(type, superType))
		}
	}
}
