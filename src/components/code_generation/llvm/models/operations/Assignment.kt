package components.code_generation.llvm.models.operations

import components.code_generation.llvm.ValueConverter
import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.models.values.SuperReference
import components.code_generation.llvm.models.values.Value
import components.code_generation.llvm.models.values.VariableValue
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.operations.Assignment
import components.semantic_model.values.Operator
import errors.internal.CompilerError
import java.util.*

class Assignment(override val model: Assignment, val targets: List<Value>, val sourceExpression: Value):
	Unit(model, listOf(*targets.toTypedArray(), sourceExpression)) {

	override fun compile(constructor: LlvmConstructor) {
		val rawLlvmValue = sourceExpression.getLlvmValue(constructor)
		for(target in targets) {
			val conversion = model.conversions[target.model]
			//TODO optimization: cache converted value to avoid re-wrapping or re-boxing
			// cache key: targetType & isTargetGeneric & conversion
			when(target) {
				is VariableValue -> {
					val declaration = target.model.declaration
					val llvmValue = ValueConverter.convertIfRequired(model, constructor, rawLlvmValue, sourceExpression.model.effectiveType,
						sourceExpression.model.hasGenericType, declaration?.effectiveType, false, conversion)
					if(declaration is ComputedPropertyDeclaration)
						buildSetterCall(constructor, declaration, context.getThisParameter(constructor), llvmValue)
					else
						constructor.buildStore(llvmValue, target.getLlvmLocation(constructor))
				}
				is MemberAccess -> {
					val memberAccess = target
					val declaration = (memberAccess.member as? VariableValue)?.model?.declaration
					val memberAccessTargetValue = memberAccess.target.getLlvmValue(constructor)
					val llvmValue = ValueConverter.convertIfRequired(model, constructor, rawLlvmValue, sourceExpression.model.effectiveType,
						sourceExpression.model.hasGenericType, memberAccess.member.model.effectiveType,
						memberAccess.member.model.effectiveType != declaration?.effectiveType, conversion)
					if(memberAccess.model.isOptional) {
						val function = constructor.getParentFunction()
						val writeBlock = constructor.createBlock(function, "optionalMemberAccess_write")
						val endBlock = constructor.createBlock(function, "optionalMemberAccess_end")
						constructor.buildJump(constructor.buildIsNull(memberAccessTargetValue, "_isTargetNull"), endBlock, writeBlock)
						constructor.select(writeBlock)
						//TODO fix: member access target value is calculated twice if not computed property (write test!)
						if(declaration is ComputedPropertyDeclaration)
							buildSetterCall(constructor, declaration, memberAccessTargetValue, llvmValue)
						else
							constructor.buildStore(llvmValue, memberAccess.getLlvmLocation(constructor))
						constructor.buildJump(endBlock)
						constructor.select(endBlock)
					} else {
						if(declaration is ComputedPropertyDeclaration)
							buildSetterCall(constructor, declaration, memberAccessTargetValue, llvmValue)
						else
							constructor.buildStore(llvmValue, memberAccess.getLlvmLocation(constructor))
					}
				}
				is IndexAccess -> {
					val targetType = target.model.targetSignature?.parameterTypes?.lastOrNull()
					val llvmValue = ValueConverter.convertIfRequired(model, constructor, rawLlvmValue, sourceExpression.model.effectiveType,
						sourceExpression.model.hasGenericType, targetType,
						targetType != target.model.targetSignature?.original?.parameterTypes?.lastOrNull(), conversion)
					compileAssignmentToIndexAccess(constructor, target, llvmValue)
				}
				else -> throw CompilerError(model, "Target of type '${target.javaClass.simpleName}' is not assignable.")
			}
		}
	}

	private fun buildSetterCall(constructor: LlvmConstructor, declaration: ComputedPropertyDeclaration, targetValue: LlvmValue,
								sourceValue: LlvmValue) {
		val exceptionAddress = context.getExceptionParameter(constructor)
		val parameters = LinkedList<LlvmValue>()
		parameters.add(exceptionAddress)
		parameters.add(targetValue)
		parameters.add(sourceValue)
		val functionAddress = context.resolveFunction(constructor, targetValue, declaration.setterIdentifier)
		constructor.buildFunctionCall(declaration.computedPropertyUnit.llvmSetterType, functionAddress, parameters)
		context.continueRaise(constructor, model)
	}

	private fun compileAssignmentToIndexAccess(constructor: LlvmConstructor, indexAccess: IndexAccess, value: LlvmValue) {
		val signature = indexAccess.model.targetSignature?.original
			?: throw CompilerError(model, "Missing index operator implementation.")
		val indexTarget = indexAccess.target
		val targetValue = indexTarget.getLlvmValue(constructor) //TODO convert (write test)
		val indexOperatorAddress = if(indexTarget is SuperReference) {
			val implementation = signature.associatedImplementation
				?: throw CompilerError(model, "Encountered member signature without implementation.")
			implementation.unit.llvmValue
		} else {
			context.resolveFunction(constructor, targetValue, signature.getIdentifier(Operator.Kind.BRACKETS_SET))
		}
		val parameters = LinkedList<LlvmValue>()
		parameters.add(context.getExceptionParameter(constructor))
		parameters.add(targetValue)
		for((indexIndex, indexValue) in indexAccess.indices.withIndex()) {
			val parameterType = signature.getParameterTypeAt(indexIndex)
			parameters.add(
				ValueConverter.convertIfRequired(model, constructor, indexValue.getLlvmValue(constructor), indexValue.model.effectiveType,
					indexValue.model.hasGenericType, parameterType, parameterType != signature.original.getParameterTypeAt(indexIndex)))
		}
		parameters.add(value)
		constructor.buildFunctionCall(signature.getLlvmType(constructor), indexOperatorAddress, parameters)
		context.continueRaise(constructor, model)
	}

}
