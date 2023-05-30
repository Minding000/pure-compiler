package components.compiler.targets.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM.*

object Llvm {
	const val NO = 0
	const val YES = 1
	const val OK = 0

	fun createContext(): LlvmContextReference = LLVMContextCreate()

	fun createModule(context: LlvmContextReference, name: String): LlvmModuleReference = LLVMModuleCreateWithNameInContext(name, context)

	fun createBuilder(context: LlvmContextReference): LlvmBuilderReference = LLVMCreateBuilderInContext(context)

	fun create32BitIntegerType(context: LlvmContextReference): LlvmTypeReference = LLVMInt32TypeInContext(context)

	fun createVoidType(context: LlvmContextReference): LlvmTypeReference = LLVMVoidTypeInContext(context)

	fun buildReturn(context: LlvmContext, value: LlvmValueReference? = null): LlvmValueReference {
		if(value == null)
			return LLVMBuildRetVoid(context.builder)
		return LLVMBuildRet(context.builder, value)
	}

	fun buildInt32(context: LlvmContext, value: Long): LlvmValueReference {
		return LLVMConstInt(context.i32Type, value, NO)
	}

	fun buildFunctionType(argumentTypes: LlvmList<LlvmTypeReference>, argumentCount: Int,
						  returnType: LlvmTypeReference?): LlvmTypeReference {
		return LLVMFunctionType(returnType, argumentTypes, argumentCount, NO)
	}

	fun buildFunction(context: LlvmContext, name: String, type: LlvmTypeReference): LlvmValueReference {
		val function = LLVMAddFunction(context.module, name, type)
		LLVMSetFunctionCallConv(function, LLVMCCallConv)
		return function
	}

	fun createBlock(context: LlvmContext, value: LlvmValueReference, name: String) {
		val block = LLVMAppendBasicBlockInContext(context.context, value, name)
		LLVMPositionBuilderAtEnd(context.builder, block)
	}
}

typealias LlvmValueReference = LLVMValueRef
typealias LlvmContextReference = LLVMContextRef
typealias LlvmModuleReference = LLVMModuleRef
typealias LlvmBuilderReference = LLVMBuilderRef
typealias LlvmTypeReference = LLVMTypeRef
typealias LlvmList<T> = PointerPointer<T>
