package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.initialization.CircularAssignment
import logger.issues.modifiers.OverridingPropertyTypeMismatch
import logger.issues.modifiers.OverridingPropertyTypeNotAssignable
import logger.issues.modifiers.VariablePropertyOverriddenByValue

open class PropertyDeclaration(source: Element, scope: Scope, name: String, type: Type? = null, value: Value? = null,
							   isStatic: Boolean = false, isAbstract: Boolean = false, isConstant: Boolean = true,
							   isMutable: Boolean = false, isOverriding: Boolean = false):
	InterfaceMember(source, scope, name, type, value, isStatic, isAbstract, isConstant, isMutable, isOverriding) {

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): PropertyDeclaration {
		return PropertyDeclaration(source, scope, name, type?.withTypeSubstitutions(linter, typeSubstitutions), value, isStatic, isAbstract,
			isConstant, isMutable, isOverriding)
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
		val superMember = superMember
		if(superMember !== null) {
			val type = type
			val superType = superMember.type
			if(type !== null && superType !== null) {
				if(isConstant) {
					if(!type.isAssignableTo(superType))
						linter.addIssue(OverridingPropertyTypeNotAssignable(type, superType))
				} else {
					if(type != superType)
						linter.addIssue(OverridingPropertyTypeMismatch(type, superType))
				}
			}
			if(!superMember.isConstant && isConstant)
				linter.addIssue(VariablePropertyOverriddenByValue(this))
		}
	}
}
