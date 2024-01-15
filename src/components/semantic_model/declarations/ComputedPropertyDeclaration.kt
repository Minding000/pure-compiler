package components.semantic_model.declarations

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmType
import components.code_generation.llvm.LlvmValue
import components.semantic_model.context.SpecialType
import components.semantic_model.control_flow.ReturnStatement
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.general.SemanticModel
import components.semantic_model.general.StatementBlock
import components.semantic_model.scopes.BlockScope
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.FunctionType
import components.semantic_model.types.LiteralType
import components.semantic_model.types.Type
import components.semantic_model.values.Value
import logger.issues.modifiers.OverridingMemberKindMismatch
import logger.issues.modifiers.OverridingPropertyTypeMismatch
import logger.issues.modifiers.OverridingPropertyTypeNotAssignable
import logger.issues.modifiers.VariablePropertyOverriddenByValue
import components.syntax_parser.syntax_tree.definitions.ComputedPropertyDeclaration as ComputedPropertySyntaxTree

class ComputedPropertyDeclaration(override val source: ComputedPropertySyntaxTree, scope: MutableScope, name: String, type: Type?,
								  val whereClauseConditions: List<WhereClauseCondition>, isOverriding: Boolean, isAbstract: Boolean,
								  val getterScope: BlockScope, val setterScope: BlockScope, getter: SemanticModel?,
								  val setter: SemanticModel?):
	PropertyDeclaration(source, scope, name, type, getter as? Value, false, isAbstract, setter == null, false,
		isOverriding) {
	val getterIdentifier
		get() = "get $memberIdentifier"
	val setterIdentifier
		get() = "set $memberIdentifier"
	var llvmGetterType: LlvmType? = null
	var llvmSetterType: LlvmType? = null
	var llvmGetterValue: LlvmValue? = null
	var llvmSetterValue: LlvmValue? = null
	val getterErrorHandlingContext: ErrorHandlingContext?
	val setterErrorHandlingContext: ErrorHandlingContext?
	val getterReturnType: Type?
		get() = type
	val setterReturnType = LiteralType(source, scope, SpecialType.NOTHING)

	init {
		getterScope.semanticModel = this
		setterScope.semanticModel = this
		getterErrorHandlingContext = when(getter) {
			null -> null
			is ErrorHandlingContext -> getter
			else -> {
				val mainBlockScope = BlockScope(getterScope)
				val returnStatement = ReturnStatement(getter.source, mainBlockScope, getter as? Value)
				val mainBlock = StatementBlock(getter.source, mainBlockScope, returnStatement)
				ErrorHandlingContext(getter.source, getterScope, mainBlock)
			}
		}
		setterErrorHandlingContext = when(setter) {
			null -> null
			is ErrorHandlingContext -> setter
			else -> {
				val mainBlock = StatementBlock(setter.source, BlockScope(setterScope), setter)
				ErrorHandlingContext(setter.source, setterScope, mainBlock)
			}
		}
		addSemanticModels(whereClauseConditions)
		addSemanticModels(setterReturnType, getterErrorHandlingContext, setterErrorHandlingContext)
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
		if(getterErrorHandlingContext != null) {
			val functionType = constructor.buildFunctionType(listOf(constructor.pointerType, constructor.pointerType), llvmType)
			llvmGetterType = functionType
			llvmGetterValue = constructor.buildFunction(getterIdentifier, functionType)
		}
		if(setterErrorHandlingContext != null) {
			val functionType = constructor.buildFunctionType(listOf(constructor.pointerType, constructor.pointerType, llvmType))
			llvmSetterType = functionType
			llvmSetterValue = constructor.buildFunction(setterIdentifier, functionType)
		}
	}

	override fun compile(constructor: LlvmConstructor) {
		val previousBlock = constructor.getCurrentBlock()
		val llvmGetterValue = llvmGetterValue
		if(llvmGetterValue != null && getterErrorHandlingContext != null) {
			constructor.createAndSelectEntrypointBlock(llvmGetterValue)
			getterErrorHandlingContext.compile(constructor)
		}
		val llvmSetterValue = llvmSetterValue
		if(llvmSetterValue != null && setterErrorHandlingContext != null) {
			constructor.createAndSelectEntrypointBlock(llvmSetterValue)
			setterErrorHandlingContext.compile(constructor)
			if(!setterErrorHandlingContext.isInterruptingExecutionBasedOnStructure)
				constructor.buildReturn()
		}
		constructor.select(previousBlock)
	}
}
