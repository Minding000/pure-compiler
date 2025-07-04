package components.semantic_model.declarations

import components.code_generation.llvm.models.declarations.PropertyDeclaration
import components.code_generation.llvm.models.declarations.ValueDeclaration
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.FunctionType
import components.semantic_model.types.Type
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.modifiers.OverridingMemberKindMismatch
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

	protected open fun validateSuperMember() {
		val (superMember, superMemberType) = superMember ?: return
		if(!superMember.isConstant && isConstant)
			context.addIssue(VariablePropertyOverriddenByValue(this))
		val type = providedType ?: return
		if(superMemberType == null)
			return
		if(type is FunctionType) {
			if(superMember is ComputedPropertyDeclaration)
				context.addIssue(OverridingMemberKindMismatch(source, name, "function", "computed property"))
			else if(superMemberType !is FunctionType)
				context.addIssue(OverridingMemberKindMismatch(source, name, "function", "property"))
			return
		}
		if(superMemberType is FunctionType) {
			context.addIssue(OverridingMemberKindMismatch(source, name, "property", "function"))
			return
		}
		if(superMember is ComputedPropertyDeclaration) {
			context.addIssue(OverridingMemberKindMismatch(source, name, "property", "computed property"))
			return
		}
		if(isConstant) {
			if(!type.isAssignableTo(superMemberType))
				context.addIssue(OverridingPropertyTypeNotAssignable(type, superMemberType))
		} else {
			if(type != superMemberType)
				context.addIssue(OverridingPropertyTypeMismatch(type, superMemberType))
		}
	}

	override fun toUnit(): ValueDeclaration {
		val unit = PropertyDeclaration(this, value?.toUnit())
		this.unit = unit
		return unit
	}
}
