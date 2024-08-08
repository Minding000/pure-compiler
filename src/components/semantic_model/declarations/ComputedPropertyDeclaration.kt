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
import errors.internal.CompilerError
import logger.issues.declaration.MissingBody
import logger.issues.modifiers.OverridingMemberKindMismatch
import logger.issues.modifiers.OverridingPropertyTypeMismatch
import logger.issues.modifiers.OverridingPropertyTypeNotAssignable
import logger.issues.modifiers.VariablePropertyOverriddenByValue
import components.syntax_parser.syntax_tree.definitions.ComputedPropertyDeclaration as ComputedPropertySyntaxTree

//TODO allow read/write base on 'gettable' and 'settable' modifiers for abstract and native computed properties
//TODO disallow body for abstract and native computed properties
class ComputedPropertyDeclaration(override val source: ComputedPropertySyntaxTree, scope: MutableScope, name: String, type: Type?,
								  val whereClauseConditions: List<WhereClauseCondition>, isOverriding: Boolean, isAbstract: Boolean,
								  val isNative: Boolean, val isGettable: Boolean, val isSettable: Boolean, val getterScope: BlockScope,
								  val setterScope: BlockScope, getter: SemanticModel?, val setter: SemanticModel?):
	PropertyDeclaration(source, scope, name, type, null, false, isAbstract, setter == null, false,
		isOverriding) {
	override val value: Value? = getter as? Value
	val root: ComputedPropertyDeclaration
		get() = superComputedProperty?.root ?: this
	var superComputedProperty: ComputedPropertyDeclaration? = null
	val hasGenericType: Boolean
		get() = effectiveType != root.effectiveType
	val getterIdentifier: String
		get() = superComputedProperty?.getterIdentifier ?: "get $memberIdentifier"
	val setterIdentifier: String
		get() = superComputedProperty?.setterIdentifier ?: "set $memberIdentifier"
	var llvmGetterType: LlvmType? = null
	var llvmSetterType: LlvmType? = null
	var llvmGetterValue: LlvmValue? = null
	var llvmSetterValue: LlvmValue? = null
	val getterErrorHandlingContext: ErrorHandlingContext?
	val setterErrorHandlingContext: ErrorHandlingContext?
	val getterReturnType: Type?
		get() = providedType
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

	override fun validate() {
		super.validate()
		validateBody()
	}

	override fun validateSuperMember() {
		val (superMember, superMemberType) = superMember ?: return
		if(!superMember.isConstant && isConstant)
			context.addIssue(VariablePropertyOverriddenByValue(this))
		val type = providedType ?: return
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
		superComputedProperty = superMember
		if(isConstant) {
			if(!type.isAssignableTo(superMemberType))
				context.addIssue(OverridingPropertyTypeNotAssignable(type, superMemberType))
		} else {
			if(type != superMemberType)
				context.addIssue(OverridingPropertyTypeMismatch(type, superMemberType))
		}
	}

	private fun validateBody() {
		if(isAbstract || isNative)
			return
		if(getterErrorHandlingContext == null && setterErrorHandlingContext == null)
			context.addIssue(MissingBody(source, "Computed property", "$name: $providedType"))
	}

	override fun declare(constructor: LlvmConstructor) {
		super.declare(constructor)
		val llvmType = root.effectiveType?.getLlvmType(constructor)
		if(isGettable || getterErrorHandlingContext != null) {
			val functionType = constructor.buildFunctionType(listOf(constructor.pointerType, constructor.pointerType), llvmType)
			llvmGetterType = functionType
			if(!isAbstract)
				llvmGetterValue = constructor.buildFunction(getterIdentifier, functionType)
		}
		if(isSettable || setterErrorHandlingContext != null) {
			val functionType = constructor.buildFunctionType(listOf(constructor.pointerType, constructor.pointerType, llvmType))
			llvmSetterType = functionType
			if(!isAbstract)
				llvmSetterValue = constructor.buildFunction(setterIdentifier, functionType)
		}
	}

	override fun compile(constructor: LlvmConstructor) {
		if(isAbstract)
			return
		val previousBlock = constructor.getCurrentBlock()
		if(isNative) {
			if(isGettable) {
				val llvmGetterValue = llvmGetterValue ?: throw CompilerError(source, "Missing getter value")
				context.nativeRegistry.compileNativeImplementation(constructor, source, "computed property getter", getterIdentifier,
					llvmGetterValue)
			}
			if(isSettable) {
				val llvmSetterValue = llvmSetterValue ?: throw CompilerError(source, "Missing setter value")
				context.nativeRegistry.compileNativeImplementation(constructor, source, "computed property setter", setterIdentifier,
					llvmSetterValue)
			}
			constructor.select(previousBlock)
			return
		}
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
