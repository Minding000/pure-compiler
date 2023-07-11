package components.compiler.targets.llvm

import errors.internal.CompilerError
import org.bytedeco.llvm.global.LLVM.*
import util.toLlvmList

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

	fun buildInt32(value: Int): LlvmValue = buildInt32(value.toLong())
	fun buildInt32(value: Long): LlvmValue {
		return LLVMConstInt(i32Type, value, Llvm.NO)
	}

	fun buildFloat(value: Double): LlvmValue {
		return LLVMConstReal(floatType, value)
	}

	fun declareStruct(name: String): LlvmType {
		return LLVMStructCreateNamed(context, name)
	}

	fun defineStruct(structType: LlvmType, memberTypes: List<LlvmType?>) {
		LLVMStructSetBody(structType, memberTypes.toLlvmList(), memberTypes.size, Llvm.NO)
	}

	fun createPointerType(baseType: LlvmType): LlvmType {
		return LLVMPointerType(baseType, Llvm.DEFAULT_ADDRESS_SPACE_INDEX)
	}

	fun buildArray(type: LlvmType, size: LlvmValue, name: String): LlvmValue {
		return LLVMBuildArrayMalloc(builder, type, size, name)
	}

	fun buildFunctionType(parameterTypes: List<LlvmType?> = emptyList(), returnType: LlvmType? = voidType): LlvmType {
		if(returnType == null)
			throw CompilerError("Missing return type in function.")
		return LLVMFunctionType(returnType, parameterTypes.toLlvmList(), parameterTypes.size, Llvm.NO)
	}

	fun buildFunction(name: String, type: LlvmType = buildFunctionType()): LlvmValue {
		val function = LLVMAddFunction(module, name, type)
		LLVMSetFunctionCallConv(function, LLVMCCallConv)
		return function
	}

	fun buildFunctionCall(functionType: LlvmType, function: LlvmValue, parameters: List<LlvmValue> = emptyList(), name: String = ""): LlvmValue {
		return LLVMBuildCall2(builder, functionType, function, parameters.toLlvmList(), parameters.size, name)
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

	fun buildGlobal(name: String, type: LlvmType?, initialValue: LlvmValue): LlvmValue {
		if(type == null)
			throw CompilerError("Missing type in allocation '$name'.")
		val global = LLVMAddGlobal(module, type, name)
		LLVMSetInitializer(global, initialValue)
		return global
	}

	fun buildConstantStruct(type: LlvmType?, values: List<LlvmValue>): LlvmValue {
		if(type == null)
			throw CompilerError("Missing type for constant struct.")
		return LLVMConstNamedStruct(type, values.toLlvmList(), values.size)
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

	fun buildLoad(type: LlvmType?, location: LlvmValue?, name: String): LlvmValue {
		if(type == null)
			throw CompilerError("Missing type in load '$name'.")
		if(location == null)
			throw CompilerError("Missing location in load '$name'.")
		return LLVMBuildLoad2(builder, type, location, name)
	}

	fun buildGetPropertyPointer(structType: LlvmType?, structPointer: LlvmValue, propertyIndex: Int, name: String): LlvmValue {
		if(structType == null)
			throw CompilerError("Missing struct type in getelementptr '$name'.")
		return LLVMBuildStructGEP2(builder, structType, structPointer, propertyIndex, name)
	}

	fun buildGetArrayElementPointer(arrayPointer: LlvmValue, elementIndex: LlvmValue, name: String): LlvmValue {
		val indices = listOf(elementIndex)
		return LLVMBuildGEP2(builder, LLVMTypeOf(arrayPointer), arrayPointer, indices.toLlvmList(), indices.size, name) //TODO replace LLVMTypeOf with type parameter
	}

	fun buildCastFromSignedIntegerToFloat(integer: LlvmValue, name: String): LlvmValue = LLVMBuildSIToFP(builder, integer, floatType, name)

	fun buildBooleanNegation(value: LlvmValue, name: String): LlvmValue = LLVMBuildNot(builder, value, name)
	fun buildAnd(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildAnd(builder, left, right, name)
	fun buildOr(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildOr(builder, left, right, name)
	fun buildBooleanEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.BooleanOperation.EQUAL_TO, left, right, name)
	fun buildBooleanNotEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.BooleanOperation.NOT_EQUAL_TO, left, right, name)

	fun buildIntegerNegation(value: LlvmValue, name: String): LlvmValue = LLVMBuildNeg(builder, value, name)
	fun buildIntegerAddition(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildAdd(builder, left, right, name)
	fun buildIntegerSubtraction(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildSub(builder, left, right, name)
	fun buildIntegerMultiplication(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildMul(builder, left, right, name)
	fun buildSignedIntegerDivision(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildSDiv(builder, left, right, name)
	fun buildSignedIntegerLessThan(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.SignedIntegerOperation.LESS_THAN, left, right, name)
	fun buildSignedIntegerGreaterThan(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.SignedIntegerOperation.GREATER_THAN, left, right, name)
	fun buildSignedIntegerLessThanOrEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.SignedIntegerOperation.LESS_THAN_OR_EQUAL_TO, left, right, name)
	fun buildSignedIntegerGreaterThanOrEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.SignedIntegerOperation.GREATER_THAN_OR_EQUAL_TO, left, right, name)
	fun buildSignedIntegerEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.SignedIntegerOperation.EQUAL_TO, left, right, name)
	fun buildSignedIntegerNotEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildICmp(builder, Llvm.SignedIntegerOperation.NOT_EQUAL_TO, left, right, name)

	fun buildFloatNegation(value: LlvmValue, name: String): LlvmValue = LLVMBuildFNeg(builder, value, name)
	fun buildFloatAddition(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildFAdd(builder, left, right, name)
	fun buildFloatSubtraction(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildFSub(builder, left, right, name)
	fun buildFloatMultiplication(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildFMul(builder, left, right, name)
	fun buildFloatDivision(left: LlvmValue, right: LlvmValue, name: String): LlvmValue = LLVMBuildFDiv(builder, left, right, name)
	fun buildFloatLessThan(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildFCmp(builder, Llvm.FloatOperation.LESS_THAN, left, right, name)
	fun buildFloatGreaterThan(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildFCmp(builder, Llvm.FloatOperation.GREATER_THAN, left, right, name)
	fun buildFloatLessThanOrEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildFCmp(builder, Llvm.FloatOperation.LESS_THAN_OR_EQUAL_TO, left, right, name)
	fun buildFloatGreaterThanOrEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildFCmp(builder, Llvm.FloatOperation.GREATER_THAN_OR_EQUAL_TO, left, right, name)
	fun buildFloatEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildFCmp(builder, Llvm.FloatOperation.EQUAL_TO, left, right, name)
	fun buildFloatNotEqualTo(left: LlvmValue, right: LlvmValue, name: String): LlvmValue =
		LLVMBuildFCmp(builder, Llvm.FloatOperation.NOT_EQUAL_TO, left, right, name)

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
