package components.code_generation.llvm.native_implementations.primitives

import components.code_generation.llvm.context.NativeRegistry
import components.code_generation.llvm.context.PrimitiveImplementation
import components.code_generation.llvm.wrapper.LlvmConstructor
import components.semantic_model.context.Context

class PrimitiveIntNatives(val context: Context) {

	fun load(registry: NativeRegistry, constructor: LlvmConstructor) {
		registry.registerPrimitiveImplementation("Int.toThePowerOf(Int): Int", compileToThePowerOf(constructor))
	}

	private fun compileToThePowerOf(constructor: LlvmConstructor): PrimitiveImplementation {
		val functionType =
			constructor.buildFunctionType(listOf(constructor.pointerType, constructor.i32Type, constructor.i32Type), constructor.i32Type)
		val function = constructor.buildFunction("Int.toThePowerOf(Int): Int", functionType)
		constructor.createAndSelectEntrypointBlock(function)
		val thisInt = context.getThisParameter(constructor)
		val baseVariable = constructor.buildStackAllocation(constructor.i32Type, "baseVariable", thisInt)
		val parameterInt = constructor.getParameter(Context.VALUE_PARAMETER_OFFSET)
		val exponentVariable = constructor.buildStackAllocation(constructor.i32Type, "exponentVariable", parameterInt)
		val resultVariable = constructor.buildStackAllocation(constructor.i32Type, "resultVariable", constructor.buildInt32(1))
		val loopConditionBlock = constructor.createBlock(function, "loop_condition")
		val loopBodyBlock = constructor.createBlock(function, "loop_body")
		val loopExitBlock = constructor.createBlock(function, "loop_exit")
		val ifBodyBlock = constructor.createBlock(function, "if_body")
		val ifExitBlock = constructor.createBlock(function, "if_exit")
		constructor.buildJump(loopConditionBlock)
		constructor.select(loopConditionBlock)
		run {
			val exponent = constructor.buildLoad(constructor.i32Type, exponentVariable, "exponent")
			val condition = constructor.buildSignedIntegerGreaterThan(exponent, constructor.buildInt32(0), "condition")
			constructor.buildJump(condition, loopBodyBlock, loopExitBlock)
		}
		constructor.select(loopBodyBlock)
		run {
			val exponent = constructor.buildLoad(constructor.i32Type, exponentVariable, "exponent")
			val mostInsignificantBit = constructor.buildAnd(exponent, constructor.buildInt32(1), "mostInsignificantBit")
			val condition = constructor.buildSignedIntegerGreaterThan(mostInsignificantBit, constructor.buildInt32(0),
				"condition")
			constructor.buildJump(condition, ifBodyBlock, ifExitBlock)
		}
		constructor.select(ifBodyBlock)
		run {
			val result = constructor.buildLoad(constructor.i32Type, resultVariable, "result")
			val base = constructor.buildLoad(constructor.i32Type, baseVariable, "base")
			val newResult = constructor.buildIntegerMultiplication(result, base, "newBase")
			constructor.buildStore(newResult, resultVariable)
		}
		constructor.buildJump(ifExitBlock)
		constructor.select(ifExitBlock)
		run {
			val base = constructor.buildLoad(constructor.i32Type, baseVariable, "base")
			val newBase = constructor.buildIntegerMultiplication(base, base, "newBase")
			constructor.buildStore(newBase, baseVariable)
			val exponent = constructor.buildLoad(constructor.i32Type, exponentVariable, "exponent")
			val newExponent = constructor.buildIntegerRightShift(exponent, constructor.buildInt32(1), "newExponent")
			constructor.buildStore(newExponent, exponentVariable)
		}
		constructor.buildJump(loopConditionBlock)
		constructor.select(loopExitBlock)
		val result = constructor.buildLoad(constructor.i32Type, resultVariable, "result")
		constructor.buildReturn(result)
		return PrimitiveImplementation(function, functionType)
	}
}
