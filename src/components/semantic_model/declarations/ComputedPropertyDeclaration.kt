package components.semantic_model.declarations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.code_generation.llvm.LlvmValue
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.FunctionType
import components.semantic_model.types.Type
import components.semantic_model.values.Value
import logger.issues.modifiers.OverridingMemberKindMismatch
import logger.issues.modifiers.OverridingPropertyTypeMismatch
import logger.issues.modifiers.OverridingPropertyTypeNotAssignable
import logger.issues.modifiers.VariablePropertyOverriddenByValue
import components.syntax_parser.syntax_tree.definitions.ComputedPropertyDeclaration as ComputedPropertySyntaxTree

class ComputedPropertyDeclaration(override val source: ComputedPropertySyntaxTree, scope: MutableScope, name: String, type: Type?,
								  isOverriding: Boolean, isAbstract: Boolean, val getExpression: Value?, val setStatement: SemanticModel?):
	PropertyDeclaration(source, scope, name, type, getExpression, false, isAbstract, setStatement == null,
		false, isOverriding) {
	val getterIdentifier
		get() = "get $memberIdentifier"
	val setterIdentifier
		get() = "set $memberIdentifier"
	var llvmGetterType: LlvmType? = null
	var llvmSetterType: LlvmType? = null
	var llvmGetterValue: LlvmValue? = null
	var llvmSetterValue: LlvmValue? = null

	init {
		addSemanticModels(setStatement)
	}

	override fun validateSuperMember() {
		val (superMember, superMemberType) = superMember ?: return
		if(!superMember.isConstant && isConstant)
			context.addIssue(VariablePropertyOverriddenByValue(this))
		val type = type ?: return
		if(superMemberType == null)
			return
		if(superMemberType is FunctionType) {
			context.addIssue(OverridingMemberKindMismatch(source, name, "computed property", "function"))
			return
		}
		if(superMember !is ComputedPropertyDeclaration) {
			context.addIssue(OverridingMemberKindMismatch(source, name, "computed property", "property"))
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

	override fun declare(constructor: LlvmConstructor) {
		super.declare(constructor)
		val llvmType = type?.getLlvmType(constructor)
		if(getExpression != null) {
			val functionType = constructor.buildFunctionType(listOf(constructor.pointerType, constructor.pointerType), llvmType)
			llvmGetterType = functionType
			llvmGetterValue = constructor.buildFunction(getterIdentifier, functionType)
		}
		if(setStatement != null) {
			val functionType = constructor.buildFunctionType(listOf(constructor.pointerType, constructor.pointerType, llvmType))
			llvmSetterType = functionType
			llvmSetterValue = constructor.buildFunction(setterIdentifier, functionType)
		}
	}

	override fun compile(constructor: LlvmConstructor) {
		val previousBlock = constructor.getCurrentBlock()
		val llvmGetterValue = llvmGetterValue
		if(llvmGetterValue != null && getExpression != null) {
			constructor.createAndSelectBlock(llvmGetterValue, "entrypoint")
			constructor.buildReturn(getExpression.getLlvmValue(constructor))
		}
		val llvmSetterValue = llvmSetterValue
		if(llvmSetterValue != null && setStatement != null) {
			constructor.createAndSelectBlock(llvmSetterValue, "entrypoint")
			setStatement.compile(constructor)
			constructor.buildReturn()
		}
		constructor.select(previousBlock)
	}
}
