package components.compiler.targets.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM.*

@Suppress("MemberVisibilityCanBePrivate", "unused")
object Llvm {
	const val NO = 0
	const val YES = 1
	const val OK = 0
	const val LESS_THAN_SIGNED = LLVMIntSLT
	const val GREATER_THAN_SIGNED = LLVMIntSGT
	const val LESS_THAN_OR_EQUAL_TO_SIGNED = LLVMIntSLE
	const val GREATER_THAN_OR_EQUAL_TO_SIGNED = LLVMIntSGE
	const val EQUAL_TO = LLVMIntEQ
	const val NOT_EQUAL_TO = LLVMIntNE

	fun createContext(): LlvmContext = LLVMContextCreate()

	fun createModule(context: LlvmContext, name: String): LlvmModule = LLVMModuleCreateWithNameInContext(name, context)

	fun createBuilder(context: LlvmContext): LlvmBuilder = LLVMCreateBuilderInContext(context)

	fun create1BitIntegerType(context: LlvmContext): LlvmType = LLVMInt1TypeInContext(context)

	fun create32BitIntegerType(context: LlvmContext): LlvmType = LLVMInt32TypeInContext(context)

	fun createVoidType(context: LlvmContext): LlvmType = LLVMVoidTypeInContext(context)

	fun runFunction(engine: LlvmExecutionEngine, function: LlvmValue): LlvmGenericValue {
		return runFunction(engine, function, LlvmList(0), 0)
	}

	fun runFunction(engine: LlvmExecutionEngine, function: LlvmValue, arguments: LlvmList<LlvmValue>, argumentCount: Int): LlvmGenericValue {
		return LLVMRunFunction(engine, function, argumentCount, arguments)
	}

	fun castToSignedInt(genericValue: LlvmGenericValue): Long = LLVMGenericValueToInt(genericValue, YES)
	fun castToBool(genericValue: LlvmGenericValue): Boolean = castToSignedInt(genericValue) == 1L
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
