package components.code_generation.llvm.models.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.declarations.ComputedPropertyDeclaration
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.models.values.VariableValue
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmType
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.declarations.InterfaceMember
import components.semantic_model.declarations.TypeAlias
import components.semantic_model.operations.MemberAccess
import components.semantic_model.types.StaticType
import errors.internal.CompilerError
import components.semantic_model.declarations.ComputedPropertyDeclaration as ComputedPropertyDeclarationModel

class MemberAccess(override val model: MemberAccess, val target: Value, val member: Value): Value(model, listOf(target, member)) {

	override fun getLlvmLocation(constructor: LlvmConstructor): LlvmValue {
		if(member !is VariableValue)
			throw CompilerError(model, "Member access references invalid member of type '${member.javaClass.simpleName}'.")
		if(member.model.declaration is ComputedPropertyDeclarationModel)
			throw CompilerError(model, "Computed properties do not have a location.")
		val targetType = target.model.providedType
		if(targetType is StaticType && targetType.typeDeclaration is TypeAlias) {
			val instanceLocation = member.getLlvmLocation(constructor)
			if(instanceLocation != null)
				return instanceLocation
		}
		// Assumption: Primitives don't have properties
		return context.resolveMember(constructor, target.getLlvmValue(constructor), member.model.name,
			(member.model.declaration as? InterfaceMember)?.isStatic ?: false)
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val targetType = target.model.providedType
		return if(model.isOptional && !(targetType is StaticType && targetType.typeDeclaration is TypeAlias)) {
			val resultLlvmType = constructor.pointerType
			val targetValue = target.getLlvmValue(constructor)
			val result = constructor.buildStackAllocation(resultLlvmType, "_optionalMemberAccess_resultVariable")
			val function = constructor.getParentFunction()
			val valueBlock = constructor.createBlock(function, "_optionalMemberAccess_valueBlock")
			val nullBlock = constructor.createBlock(function, "_optionalMemberAccess_nullBlock")
			val resultBlock = constructor.createBlock(function, "_optionalMemberAccess_resultBlock")
			constructor.buildJump(constructor.buildIsNull(targetValue, "_optionalMemberAccess_isTargetNull"), nullBlock, valueBlock)
			constructor.select(nullBlock)
			constructor.buildStore(constructor.nullPointer, result)
			constructor.buildJump(resultBlock)
			constructor.select(valueBlock)
			var memberValue = getMemberValue(constructor)
			if(member.model.effectiveType?.isLlvmPrimitive() == true)
				memberValue = ValueConverter.boxPrimitive(constructor, memberValue, member.model.effectiveType?.getLlvmType(constructor))
			constructor.buildStore(memberValue, result)
			constructor.buildJump(resultBlock)
			constructor.select(resultBlock)
			constructor.buildLoad(resultLlvmType, result, "_optionalMemberAccess_result")
		} else {
			getMemberValue(constructor)
		}
	}

	private fun getMemberValue(constructor: LlvmConstructor): LlvmValue {
		//TODO what about chained member accesses? e.g. player.stats.highScore
		val declaration = (member as? VariableValue)?.model?.declaration
		return if(declaration is ComputedPropertyDeclarationModel) {
			val declarationUnit = declaration.computedPropertyUnit
			val setter = declaration.setter
			if(setter != null && model.isIn(setter))
				constructor.getLastParameter()
			else
				buildGetterCall(constructor, declarationUnit)
		} else {
			constructor.buildLoad(declaration?.effectiveType?.getLlvmType(constructor), getLlvmLocation(constructor), "member")
		}
	}

	private fun buildGetterCall(constructor: LlvmConstructor, computedPropertyDeclaration: ComputedPropertyDeclaration): LlvmValue {
		val exceptionParameter = context.getExceptionParameter(constructor)
		val targetValue = target.getLlvmValue(constructor)
		val llvmType: LlvmType?
		val llvmValue: LlvmValue
		if(target.model.effectiveType?.isLlvmPrimitive() == true) {
			val primitiveImplementation =
				context.nativeRegistry.resolvePrimitiveImplementation(computedPropertyDeclaration.getGetterSignature())
			llvmType = primitiveImplementation.llvmType
			llvmValue = primitiveImplementation.llvmValue
		} else {
			llvmType = computedPropertyDeclaration.llvmGetterType
			llvmValue = context.resolveFunction(constructor, targetValue, computedPropertyDeclaration.model.getterIdentifier)
		}
		val returnValue =
			constructor.buildFunctionCall(llvmType, llvmValue, listOf(exceptionParameter, targetValue), "_computedPropertyGetterResult")
		context.continueRaise(constructor, model)
		return returnValue
	}
}
