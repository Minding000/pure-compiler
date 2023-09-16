package components.semantic_model.declarations

import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.FunctionType
import components.semantic_model.types.Type
import components.semantic_model.values.InterfaceMember
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.modifiers.OverridingPropertyTypeMismatch
import logger.issues.modifiers.OverridingPropertyTypeNotAssignable
import logger.issues.modifiers.VariablePropertyOverriddenByValue

open class PropertyDeclaration(source: SyntaxTreeNode, scope: MutableScope, name: String, type: Type? = null, value: Value? = null,
							   isStatic: Boolean = false, isAbstract: Boolean = false, isConstant: Boolean = true,
							   isMutable: Boolean = false, isOverriding: Boolean = false):
	InterfaceMember(source, scope, name, type, value, isStatic, isAbstract, isConstant, isMutable, isOverriding) {

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
		val (superMember, superMemberType) = superMember ?: return
		if(!superMember.isConstant && isConstant)
			context.addIssue(VariablePropertyOverriddenByValue(this))
		val type = type ?: return
		if(superMemberType == null)
			return
		if(type is FunctionType && superMemberType is FunctionType)
			return
		if(isConstant) {
			if(!type.isAssignableTo(superMemberType))
				context.addIssue(OverridingPropertyTypeNotAssignable(type, superMemberType))
		} else {
			if(type != superMemberType)
				context.addIssue(OverridingPropertyTypeMismatch(type, superMemberType))
		}
	}
}
