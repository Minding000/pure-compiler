package compiler.targets.llvm

import errors.internal.CompilerError
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM

class BuildContext(name: String) {
	val context: LLVMContextRef = LLVM.LLVMContextCreate()
	val module: LLVMModuleRef = LLVM.LLVMModuleCreateWithNameInContext(name, context)
	val builder: LLVMBuilderRef = LLVM.LLVMCreateBuilderInContext(context)
	val i32Type: LLVMTypeRef = LLVM.LLVMInt32TypeInContext(context)

	private var _entrypoint: LLVMValueRef? = null

	var entrypoint: LLVMValueRef
		get() = _entrypoint ?: throw CompilerError("No entrypoint in current build context.")
		set(value) {
			_entrypoint = value
		}

	fun close() {
		LLVM.LLVMDisposeBuilder(builder)
		LLVM.LLVMContextDispose(context)
	}
}
