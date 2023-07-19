package components.compiler.targets.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM.*

@Suppress("MemberVisibilityCanBePrivate", "unused")
object Llvm {
	const val NO = 0
	const val YES = 1
	const val OK = 0
	const val DEFAULT_ADDRESS_SPACE_INDEX = 0
	object BooleanOperation {
		const val EQUAL_TO = LLVMIntEQ
		const val NOT_EQUAL_TO = LLVMIntNE
	}
	object SignedIntegerOperation {
		const val LESS_THAN = LLVMIntSLT
		const val GREATER_THAN = LLVMIntSGT
		const val LESS_THAN_OR_EQUAL_TO = LLVMIntSLE
		const val GREATER_THAN_OR_EQUAL_TO = LLVMIntSGE
		const val EQUAL_TO = LLVMIntEQ
		const val NOT_EQUAL_TO = LLVMIntNE
	}
	object UnsignedIntegerOperation {
		const val LESS_THAN = LLVMIntULT
		const val GREATER_THAN = LLVMIntUGT
		const val LESS_THAN_OR_EQUAL_TO = LLVMIntULE
		const val GREATER_THAN_OR_EQUAL_TO = LLVMIntUGE
		const val EQUAL_TO = LLVMIntEQ
		const val NOT_EQUAL_TO = LLVMIntNE
	}
	object FloatOperation {
		//TODO test NaN values
		// see: https://stackoverflow.com/questions/40327806/what-are-ordered-and-unordered-llvm-cmpinst-compare-instructions
		const val LESS_THAN = LLVMRealOLT
		const val GREATER_THAN = LLVMRealOGT
		const val LESS_THAN_OR_EQUAL_TO = LLVMRealOLE
		const val GREATER_THAN_OR_EQUAL_TO = LLVMRealOGE
		const val EQUAL_TO = LLVMRealOEQ
		const val NOT_EQUAL_TO = LLVMRealONE
	}

	fun createContext(): LlvmContext = LLVMContextCreate()

	fun createModule(context: LlvmContext, name: String): LlvmModule = LLVMModuleCreateWithNameInContext(name, context)

	fun createBuilder(context: LlvmContext): LlvmBuilder = LLVMCreateBuilderInContext(context)

	fun create1BitIntegerType(context: LlvmContext): LlvmType = LLVMInt1TypeInContext(context)

	fun create8BitIntegerType(context: LlvmContext): LlvmType = LLVMInt8TypeInContext(context)

	fun create32BitIntegerType(context: LlvmContext): LlvmType = LLVMInt32TypeInContext(context)

	fun createFloatType(context: LlvmContext): LlvmType = LLVMFloatTypeInContext(context)

	fun createVoidType(context: LlvmContext): LlvmType = LLVMVoidTypeInContext(context)

	fun runFunction(engine: LlvmExecutionEngine, function: LlvmValue): LlvmGenericValue {
		return runFunction(engine, function, LlvmList(0), 0)
	}

	fun runFunction(engine: LlvmExecutionEngine, function: LlvmValue, arguments: LlvmList<LlvmValue>, argumentCount: Int): LlvmGenericValue {
		return LLVMRunFunction(engine, function, argumentCount, arguments)
	}

	fun castToFloat(genericValue: LlvmGenericValue): Double = LLVMGenericValueToFloat(LLVMFloatType(), genericValue)
	fun castToSignedInteger(genericValue: LlvmGenericValue): Long = LLVMGenericValueToInt(genericValue, YES)
	fun castToUnsignedInteger(genericValue: LlvmGenericValue): Long = LLVMGenericValueToInt(genericValue, NO)
	fun castToBoolean(genericValue: LlvmGenericValue): Boolean = castToUnsignedInteger(genericValue) == 1L

	fun bool(value: Boolean): Int = if(value) YES else NO
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
