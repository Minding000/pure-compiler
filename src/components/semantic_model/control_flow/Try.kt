package components.semantic_model.control_flow

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.code_generation.llvm.ValueConverter
import components.semantic_model.context.SpecialType
import components.semantic_model.scopes.Scope
import components.semantic_model.types.OptionalType
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.Try as TrySyntaxTree

class Try(override val source: TrySyntaxTree, scope: Scope, val expression: Value, val isOptional: Boolean): Value(source, scope) {

	init {
		addSemanticModels(expression)
	}

	override fun determineTypes() {
		super.determineTypes()
		expression.providedType?.let { expressionType ->
			providedType = if(isOptional)
				OptionalType(source, scope, expressionType) //TODO write tests: NULL / NOTHING should not be optional
			else
				expressionType
		}
	}

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		val expressionResult = expression.getLlvmValue(constructor)
		if(isOptional) {
			val exceptionParameter = context.getExceptionParameter(constructor)
			if(SpecialType.NOTHING.matches(expression.effectiveType)) {
				constructor.buildStore(constructor.nullPointer, exceptionParameter)
				return expressionResult
			}
			val exception = constructor.buildLoad(constructor.pointerType, exceptionParameter, "exception")
			val doesExceptionExist = constructor.buildIsNotNull(exception, "doesExceptionExist")
			val resultType = effectiveType?.getLlvmType(constructor)
			val resultVariable = constructor.buildStackAllocation(resultType, "resultVariable")
			val exceptionBlock = constructor.createBlock("try_exception")
			val noExceptionBlock = constructor.createBlock("try_noException")
			val resultBlock = constructor.createBlock("try_result")
			constructor.buildJump(doesExceptionExist, exceptionBlock, noExceptionBlock)
			constructor.select(exceptionBlock)
			constructor.buildStore(constructor.nullPointer, exceptionParameter)
			constructor.buildStore(constructor.nullPointer, resultVariable)
			constructor.buildJump(resultBlock)
			constructor.select(noExceptionBlock)
			constructor.buildStore(ValueConverter.convertIfRequired(this, constructor, expressionResult,
				expression.effectiveType, expression.hasGenericType, effectiveType, false), resultVariable)
			constructor.buildJump(resultBlock)
			constructor.select(resultBlock)
			return constructor.buildLoad(resultType, resultVariable, "result")
		} else {
			return expressionResult
		}
	}
}
