package components.compiler.targets.llvm

import errors.internal.CompilerError
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM.*

@Suppress("MemberVisibilityCanBePrivate")
object Llvm {
	const val NO = 0
	const val YES = 1
	const val OK = 0

	fun createContext(): LlvmContext = LLVMContextCreate()

	fun createModule(context: LlvmContext, name: String): LlvmModule = LLVMModuleCreateWithNameInContext(name, context)

	fun createBuilder(context: LlvmContext): LlvmBuilder = LLVMCreateBuilderInContext(context)

	fun create1BitIntegerType(context: LlvmContext): LlvmType = LLVMInt1TypeInContext(context)

	fun create32BitIntegerType(context: LlvmContext): LlvmType = LLVMInt32TypeInContext(context)

	fun createVoidType(context: LlvmContext): LlvmType = LLVMVoidTypeInContext(context)

	fun buildBoolean(context: LlvmCompilerContext, value: Long): LlvmValue {
		return LLVMConstInt(context.booleanType, value, NO)
	}

	fun buildInt32(context: LlvmCompilerContext, value: Long): LlvmValue {
		return LLVMConstInt(context.i32Type, value, NO)
	}

	fun buildFunctionType(argumentTypes: LlvmList<LlvmType>, argumentCount: Int,
						  returnType: LlvmType?): LlvmType {
		return LLVMFunctionType(returnType, argumentTypes, argumentCount, NO)
	}

	fun buildFunction(context: LlvmCompilerContext, name: String, type: LlvmType): LlvmValue {
		val function = LLVMAddFunction(context.module, name, type)
		LLVMSetFunctionCallConv(function, LLVMCCallConv)
		return function
	}

	fun getParentFunction(block: LlvmBlock): LlvmValue {
		return LLVMGetBasicBlockParent(block)
	}

	fun getEntryBlock(function: LlvmValue): LlvmBlock {
		return LLVMGetEntryBasicBlock(function)
	}

	fun getCurrentBlock(context: LlvmCompilerContext): LlvmBlock {
		return LLVMGetInsertBlock(context.builder)
	}

	fun createBlock(context: LlvmCompilerContext, name: String): LlvmBlock {
		return LLVMCreateBasicBlockInContext(context.context, name)
	}

	fun createBlock(context: LlvmCompilerContext, function: LlvmValue, name: String): LlvmBlock {
		return LLVMAppendBasicBlockInContext(context.context, function, name)
	}

	fun createBlockAndPositionBuilder(context: LlvmCompilerContext, function: LlvmValue, name: String): LlvmBlock {
		val block = createBlock(context, function, name)
		LLVMPositionBuilderAtEnd(context.builder, block)
		return block
	}

	fun appendTo(context: LlvmCompilerContext, block: LlvmBlock) {
		LLVMPositionBuilderAtEnd(context.builder, block)
	}

	fun addBlockToFunction(function: LlvmValue, block: LlvmBlock) {
		LLVMAppendExistingBasicBlock(function, block)
	}

	fun buildAllocation(context: LlvmCompilerContext, type: LlvmType?, name: String): LlvmValue {
		if(type == null)
			throw CompilerError("Missing type in allocation '$name'.")
		return LLVMBuildAlloca(context.builder, type, name)
	}

	fun buildStore(context: LlvmCompilerContext, value: LlvmValue, location: LlvmValue) {
		LLVMBuildStore(context.builder, value, location)
	}

	fun buildLoad(context: LlvmCompilerContext, location: LlvmValue?, name: String): LlvmValue {
		if(location == null)
			throw CompilerError("Missing location in load '$name'.")
		return LLVMBuildLoad(context.builder, location, name)
	}

	fun buildJump(context: LlvmCompilerContext, targetBlock: LlvmBlock) {
		LLVMBuildBr(context.builder, targetBlock)
	}

	fun buildJump(context: LlvmCompilerContext, condition: LlvmValue, positiveBlock: LlvmBlock, negativeBlock: LlvmBlock) {
		LLVMBuildCondBr(context.builder, condition, positiveBlock, negativeBlock)
	}

	fun buildReturn(context: LlvmCompilerContext, value: LlvmValue? = null): LlvmValue {
		if(value == null)
			return LLVMBuildRetVoid(context.builder)
		return LLVMBuildRet(context.builder, value)
	}

	fun runFunction(engine: LlvmExecutionEngine, function: LlvmValue): LlvmGenericValue {
		return runFunction(engine, function, LlvmList(0), 0)
	}

	fun runFunction(engine: LlvmExecutionEngine, function: LlvmValue, arguments: LlvmList<LlvmValue>, argumentCount: Int): LlvmGenericValue {
		return LLVMRunFunction(engine, function, argumentCount, arguments)
	}

	fun castToInt(genericValue: LlvmGenericValue): Long {
		return LLVMGenericValueToInt(genericValue, NO)
	}
}

typealias LlvmContext = LLVMContextRef
typealias LlvmModule = LLVMModuleRef
typealias LlvmBuilder = LLVMBuilderRef
typealias LlvmBlock = LLVMBasicBlockRef
typealias LlvmValue = LLVMValueRef
typealias LlvmGenericValue = LLVMGenericValueRef
typealias LlvmType = LLVMTypeRef
typealias LlvmExecutionEngine = LLVMExecutionEngineRef
typealias LlvmList<T> = PointerPointer<T>
