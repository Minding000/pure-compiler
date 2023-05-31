package components.compiler.targets.llvm

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

	fun create32BitIntegerType(context: LlvmContext): LlvmType = LLVMInt32TypeInContext(context)

	fun createVoidType(context: LlvmContext): LlvmType = LLVMVoidTypeInContext(context)

	fun buildReturn(context: LlvmCompilerContext, value: LlvmValue? = null): LlvmValue {
		if(value == null)
			return LLVMBuildRetVoid(context.builder)
		return LLVMBuildRet(context.builder, value)
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

	fun createBlock(context: LlvmCompilerContext, value: LlvmValue, name: String) {
		val block = LLVMAppendBasicBlockInContext(context.context, value, name)
		LLVMPositionBuilderAtEnd(context.builder, block)
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

typealias LlvmValue = LLVMValueRef
typealias LlvmGenericValue = LLVMGenericValueRef
typealias LlvmContext = LLVMContextRef
typealias LlvmModule = LLVMModuleRef
typealias LlvmBuilder = LLVMBuilderRef
typealias LlvmType = LLVMTypeRef
typealias LlvmExecutionEngine = LLVMExecutionEngineRef
typealias LlvmList<T> = PointerPointer<T>
