package components.code_generation.llvm

import errors.internal.CompilerError
import org.bytedeco.llvm.global.LLVM.*
import util.toLlvmList

class LlvmConstructor(name: String) {
	val context = Llvm.createContext()
	val module = Llvm.createModule(context, name)
	val dataLayout = LLVMGetModuleDataLayout(module)
	val builder = Llvm.createBuilder(context)
	val booleanType = Llvm.create1BitIntegerType(context)
	val byteType = Llvm.create8BitIntegerType(context)
	val i32Type = Llvm.create32BitIntegerType(context)
	val i64Type = Llvm.create64BitIntegerType(context)
	val floatType = Llvm.createFloatType(context)
	val voidType = Llvm.createVoidType(context)
	val pointerType = Llvm.createPointerType(context)
	val nullPointer = LLVMConstPointerNull(pointerType)

	fun setTargetTriple(targetTriple: String) {
		LLVMSetTarget(module, targetTriple)
	}

	fun getTargetTriple(): String {
		return LLVMGetTarget(module).string
	}

	fun getParameter(function: LlvmValue, index: Int): LlvmValue {
		return LLVMGetParam(function, index)
	}

	fun getLastParameter(function: LlvmValue = getParentFunction()): LlvmValue {
		return LLVMGetLastParam(function)
	}

	fun getCurrentVariadicElement(variadicList: LlvmValue, elementType: LlvmType?, name: String): LlvmValue {
		if(elementType == null)
			throw CompilerError("Missing type in variadic element access.")
		return LLVMBuildVAArg(builder, variadicList, elementType, name)
	}

	fun getParentFunction(block: LlvmBlock = getCurrentBlock()): LlvmValue {
		return LLVMGetBasicBlockParent(block)
	}

	fun getEntryBlock(function: LlvmValue): LlvmBlock {
		return LLVMGetEntryBasicBlock(function)
	}

	fun getCurrentBlock(): LlvmBlock {
		return LLVMGetInsertBlock(builder)
	}

	fun buildBoolean(value: Boolean): LlvmValue {
		return LLVMConstInt(booleanType, Llvm.bool(value).toLong(), Llvm.NO)
	}

	fun buildByte(value: Long): LlvmValue {
		return LLVMConstInt(byteType, value, Llvm.NO)
	}

	fun buildInt32(value: Int): LlvmValue = buildInt32(value.toLong())
	fun buildInt32(value: Long): LlvmValue {
		return LLVMConstInt(i32Type, value, Llvm.NO)
	}
	fun buildInt64(value: Long): LlvmValue {
		return LLVMConstInt(i64Type, value, Llvm.NO)
	}

	fun buildFloat(value: Double): LlvmValue {
		return LLVMConstReal(floatType, value)
	}

	fun getMemberOffsetInBytes(structType: LlvmType, memberIndex: Int): Long {
		return LLVMOffsetOfElement(dataLayout, structType, memberIndex)
	}

	fun declareStruct(name: String): LlvmType {
		return LLVMStructCreateNamed(context, name)
	}

	fun defineStruct(structType: LlvmType, memberTypes: List<LlvmType?>) {
		LLVMStructSetBody(structType, memberTypes.toLlvmList(), memberTypes.size, Llvm.NO)
	}

	fun buildArrayType(baseType: LlvmType, size: Int): LlvmType {
		return LLVMArrayType(baseType, size)
	}

	fun buildFunctionType(parameterTypes: List<LlvmType?> = emptyList(), returnType: LlvmType? = voidType, isVariadic: Boolean = false): LlvmType {
		if(returnType == null)
			throw CompilerError("Missing return type in function.")
		return LLVMFunctionType(returnType, parameterTypes.toLlvmList(), parameterTypes.size, Llvm.bool(isVariadic))
	}

	fun buildFunction(name: String, type: LlvmType = buildFunctionType()): LlvmValue {
		val function = LLVMAddFunction(module, name, type)
		LLVMSetFunctionCallConv(function, LLVMCCallConv)
		return function
	}

	/**
	 * Note: Work in progress
	 */
	fun addAttribute(function: LlvmValue, attributeName: String) {
		val kind = LLVMGetEnumAttributeKindForName(attributeName, attributeName.length.toLong())
		val attribute = LLVMCreateEnumAttribute(context, kind, Llvm.YES.toLong())
		LLVMAddAttributeAtIndex(function, LLVMAttributeReturnIndex, attribute)
	}

	fun buildFunctionCall(functionType: LlvmType?, function: LlvmValue, parameters: List<LlvmValue?> = emptyList()) {
		if(functionType == null)
			throw CompilerError("Missing function type in function call.")
		val name = if(LLVMGetReturnType(functionType) == voidType) "" else "__ignored"
		buildFunctionCall(functionType, function, parameters, name)
	}

	fun buildFunctionCall(functionType: LlvmType?, function: LlvmValue, parameters: List<LlvmValue?> = emptyList(), name: String):
		LlvmValue {
		if(functionType == null)
			throw CompilerError("Missing function type in function call '$name'.")
		return LLVMBuildCall2(builder, functionType, function, parameters.toLlvmList(), parameters.size, name)
	}

	fun createBlock(name: String): LlvmBlock {
		return LLVMCreateBasicBlockInContext(context, name)
	}

	fun createBlock(function: LlvmValue, name: String): LlvmBlock {
		return LLVMAppendBasicBlockInContext(context, function, name)
	}

	fun createAndSelectEntrypointBlock(function: LlvmValue) {
		createAndSelectBlock(function, "entrypoint")
	}

	fun createAndSelectBlock(function: LlvmValue, name: String): LlvmBlock {
		val block = createBlock(function, name)
		LLVMPositionBuilderAtEnd(builder, block)
		return block
	}

	fun select(block: LlvmBlock) {
		LLVMPositionBuilderAtEnd(builder, block)
	}

	fun selectStart(block: LlvmBlock) {
		val firstInstruction = LLVMGetFirstInstruction(block)
		if(firstInstruction == null)
			select(block)
		else
			LLVMPositionBuilder(builder, block, firstInstruction)
	}

	fun addBlockToFunction(function: LlvmValue, block: LlvmBlock) {
		LLVMAppendExistingBasicBlock(function, block)
	}

	fun markAsUnreachable() {
		LLVMBuildUnreachable(builder)
	}

	fun setAlignment(definition: LlvmValue, byteCount: Int) {
		LLVMSetAlignment(definition, byteCount)
	}

	fun declareGlobal(name: String, type: LlvmType?): LlvmValue {
		if(type == null)
			throw CompilerError("Missing type in global allocation '$name'.")
		return LLVMAddGlobal(module, type, name)
	}

	fun defineGlobal(global: LlvmValue, initialValue: LlvmValue) {
		LLVMSetInitializer(global, initialValue)
	}

	fun buildConstantStruct(type: LlvmType?, values: List<LlvmValue>): LlvmValue {
		if(type == null)
			throw CompilerError("Missing type for constant struct.")
		return LLVMConstNamedStruct(type, values.toLlvmList(), values.size)
	}

	fun buildHeapAllocation(type: LlvmType?, name: String): LlvmValue {
		if(type == null)
			throw CompilerError("Missing type in heap allocation '$name'.")
		return LLVMBuildMalloc(builder, type, name)
	}

	fun buildHeapArrayAllocation(elementType: LlvmType, size: LlvmValue, name: String): LlvmValue {
		return LLVMBuildArrayMalloc(builder, elementType, size, name)
	}

	fun buildStackAllocation(type: LlvmType?, name: String): LlvmValue {
		if(type == null)
			throw CompilerError("Missing type in stack allocation '$name'.")
		return LLVMBuildAlloca(builder, type, name)
	}

	/**
	 * Places stack allocation in the entry block, so it can be optimized by 'mem2reg' pass.
	 * see: https://llvm.org/docs/tutorial/MyFirstLanguageFrontend/LangImpl07.html
	 */
	fun buildStackAllocationInEntryBlock(type: LlvmType?, name: String): LlvmValue {
		if(type == null)
			throw CompilerError("Missing type in entry point stack allocation '$name'.")
		val currentBlock = getCurrentBlock()
		val function = getParentFunction(currentBlock)
		val entryBlock = getEntryBlock(function)
		selectStart(entryBlock)
		val location = LLVMBuildAlloca(builder, type, name)
		select(currentBlock)
		return location
	}

	fun buildStackArrayAllocation(elementType: LlvmType, size: LlvmValue, name: String): LlvmValue {
		return LLVMBuildArrayAlloca(builder, elementType, size, name)
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
		if(LLVMGetTypeKind(structType) != LLVMStructTypeKind)
			throw CompilerError("Trying to get property of non-struct type in '$name'.")
		return LLVMBuildStructGEP2(builder, structType, structPointer, propertyIndex, name)
	}

	fun buildGetArrayElementPointer(elementType: LlvmType, arrayPointer: LlvmValue, elementIndex: LlvmValue, name: String): LlvmValue {
		val indices = listOf(elementIndex)
		return LLVMBuildGEP2(builder, elementType, arrayPointer, indices.toLlvmList(), indices.size, name)
	}

	fun buildConstantAsciiCharArray(text: String): LlvmValue {
		return LLVMConstStringInContext(context, text, text.length, Llvm.NO)
	}

	fun buildGlobalAsciiCharArray(name: String, text: String): LlvmValue {
		val globalCharArray = declareGlobal(name, buildArrayType(byteType, text.length + 1))
		defineGlobal(globalCharArray, buildConstantAsciiCharArray(text))
		return globalCharArray
	}

	fun changeTypeAllowingDataLoss(value: LlvmValue, newType: LlvmType?, name: String): LlvmValue {
		if(newType == null)
			throw CompilerError("Missing new type in type change allowing data loss '$name'.")
		return LLVMBuildTruncOrBitCast(builder, value, newType, name)
	}

	fun buildCastFromIntegerToPointer(value: LlvmValue, name: String): LlvmValue = LLVMBuildIntToPtr(builder, value, pointerType, name)

	fun buildCastFromBooleanToByte(boolean: LlvmValue, name: String): LlvmValue = LLVMBuildIntCast(builder, boolean, byteType, name)
	fun buildCastFromIntegerToByte(integer: LlvmValue, name: String): LlvmValue = LLVMBuildIntCast(builder, integer, byteType, name)
	fun buildCastFromByteToInteger(byte: LlvmValue, name: String): LlvmValue = LLVMBuildIntCast(builder, byte, i32Type, name)

	fun buildCastFromSignedIntegerToFloat(integer: LlvmValue, name: String): LlvmValue = LLVMBuildSIToFP(builder, integer, floatType, name)

	fun buildIsNull(value: LlvmValue, name: String): LlvmValue = LLVMBuildIsNull(builder, value, name)
	fun buildIsNotNull(value: LlvmValue, name: String): LlvmValue = LLVMBuildIsNotNull(builder, value, name)

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
