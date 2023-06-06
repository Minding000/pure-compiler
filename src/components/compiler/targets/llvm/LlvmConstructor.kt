package components.compiler.targets.llvm

import errors.internal.CompilerError
import org.bytedeco.llvm.global.LLVM.*

class LlvmConstructor(name: String) {
	val context = Llvm.createContext()
	val module = Llvm.createModule(context, name)
	val builder = Llvm.createBuilder(context)
	val booleanType = Llvm.create1BitIntegerType(context)
	val i32Type = Llvm.create32BitIntegerType(context)
	val floatType = Llvm.createFloatType(context)
	val voidType = Llvm.createVoidType(context)

	fun getParameter(function: LlvmValue, index: Int): LlvmValue {
		return LLVMGetParam(function, index)
	}

	fun getParentFunction(): LlvmValue {
		return LLVMGetBasicBlockParent(getCurrentBlock())
	}

	fun getParentFunction(block: LlvmBlock): LlvmValue {
		return LLVMGetBasicBlockParent(block)
	}

	fun getEntryBlock(function: LlvmValue): LlvmBlock {
		return LLVMGetEntryBasicBlock(function)
	}

	fun getCurrentBlock(): LlvmBlock {
		return LLVMGetInsertBlock(builder)
	}

	fun buildBoolean(value: Boolean): LlvmValue {
		return LLVMConstInt(booleanType, if(value) 1 else 0, Llvm.NO)
	}

	fun buildInt32(value: Long): LlvmValue {
		return LLVMConstInt(i32Type, value, Llvm.NO)
	}

	fun buildFloat(value: Double): LlvmValue {
		return LLVMConstReal(floatType, value)
	}

	fun buildFunctionType(parameterTypes: LlvmList<LlvmType>, parameterCount: Int, returnType: LlvmType?): LlvmType {
		if(returnType == null)
			throw CompilerError("Missing return type in function.")
		return LLVMFunctionType(returnType, parameterTypes, parameterCount, Llvm.NO)
	}

	fun buildFunction(name: String, type: LlvmType): LlvmValue {
		val function = LLVMAddFunction(module, name, type)
		LLVMSetFunctionCallConv(function, LLVMCCallConv)
		return function
	}

	fun buildFunctionCall(function: LlvmValue, parameters: LlvmList<LlvmValue>, parameterCount: Int, name: String): LlvmValue {
		return LLVMBuildCall(builder, function, parameters, parameterCount, name)
	}

	fun createBlock(name: String): LlvmBlock {
		return LLVMCreateBasicBlockInContext(context, name)
	}

	fun createBlock(function: LlvmValue, name: String): LlvmBlock {
		return LLVMAppendBasicBlockInContext(context, function, name)
	}

	fun createAndSelectBlock(function: LlvmValue, name: String): LlvmBlock {
		val block = createBlock(function, name)
		LLVMPositionBuilderAtEnd(builder, block)
		return block
	}

	fun select(block: LlvmBlock) {
		LLVMPositionBuilderAtEnd(builder, block)
	}

	fun addBlockToFunction(function: LlvmValue, block: LlvmBlock) {
		LLVMAppendExistingBasicBlock(function, block)
	}

	fun buildAllocation(type: LlvmType?, name: String): LlvmValue {
		if(type == null)
			throw CompilerError("Missing type in allocation '$name'.")
		return LLVMBuildAlloca(builder, type, name)
	}

	fun buildStore(value: LlvmValue, location: LlvmValue?) {
		if(location == null)
			throw CompilerError("Missing location in store.")
		LLVMBuildStore(builder, value, location)
	}

	fun buildLoad(location: LlvmValue?, name: String): LlvmValue {
		if(location == null)
			throw CompilerError("Missing location in load '$name'.")
		return LLVMBuildLoad(builder, location, name)
	}

	fun buildCastFromSignedIntegerToFloat(integer: LlvmValue, name: String): LlvmValue = LLVMBuildSIToFP(builder, integer, floatType, name)

	fun buildNot(value: LlvmValue, name: String): LlvmValue = LLVMBuildNot(builder, value, name)
	fun buildAnd(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildAnd(builder, left, right, name)
	fun buildOr(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildOr(builder, left, right, name)
	fun buildBooleanEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = buildIntegerEqualTo(left, right, name)
	fun buildBooleanNotEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = buildIntegerNotEqualTo(left, right, name)

	fun buildIntegerNegation(value: LlvmValue, name: String): LlvmValue = LLVMBuildNeg(builder, value, name)
	fun buildIntegerAddition(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildAdd(builder, left, right, name)
	fun buildIntegerSubtraction(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildSub(builder, left, right, name)
	fun buildIntegerMultiplication(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildMul(builder, left, right, name)
	fun buildIntegerDivision(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildSDiv(builder, left, right, name)
	fun buildIntegerLessThan(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.LESS_THAN_SIGNED, left, right, name)
	fun buildIntegerGreaterThan(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.GREATER_THAN_SIGNED, left, right, name)
	fun buildIntegerLessThanOrEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.LESS_THAN_OR_EQUAL_TO_SIGNED, left, right, name)
	fun buildIntegerGreaterThanOrEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.GREATER_THAN_OR_EQUAL_TO_SIGNED, left, right, name)
	fun buildIntegerEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.EQUAL_TO, left, right, name)
	fun buildIntegerNotEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.NOT_EQUAL_TO, left, right, name)

	fun buildFloatNegation(value: LlvmValue, name: String): LlvmValue = LLVMBuildFNeg(builder, value, name)
	fun buildFloatAddition(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildFAdd(builder, left, right, name)
	fun buildFloatSubtraction(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildFSub(builder, left, right, name)
	fun buildFloatMultiplication(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildFMul(builder, left, right, name)
	fun buildFloatDivision(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildFDiv(builder, left, right, name)
	fun buildFloatLessThan(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildFCmp(builder, Llvm.LESS_THAN_SIGNED, left, right, name)
	fun buildFloatGreaterThan(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildFCmp(builder, Llvm.GREATER_THAN_SIGNED, left, right, name)
	fun buildFloatLessThanOrEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildFCmp(builder, Llvm.LESS_THAN_OR_EQUAL_TO_SIGNED, left, right, name)
	fun buildFloatGreaterThanOrEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildFCmp(builder, Llvm.GREATER_THAN_OR_EQUAL_TO_SIGNED, left, right, name)
	fun buildFloatEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildFCmp(builder, Llvm.EQUAL_TO, left, right, name)
	fun buildFloatNotEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildFCmp(builder, Llvm.NOT_EQUAL_TO, left, right, name)

	fun buildJump(targetBlock: LlvmBlock) {
		LLVMBuildBr(builder, targetBlock)
	}

	fun buildJump(condition: LlvmValue, positiveBlock: LlvmBlock, negativeBlock: LlvmBlock) {
		LLVMBuildCondBr(builder, condition, positiveBlock, negativeBlock)
	}

	fun buildReturn(value: LlvmValue? = null): LlvmValue {
		if(value == null)
			return LLVMBuildRetVoid(builder)
		return LLVMBuildRet(builder, value)
	}

	fun dispose() {
		LLVMDisposeBuilder(builder)
		LLVMContextDispose(context)
	}
}
