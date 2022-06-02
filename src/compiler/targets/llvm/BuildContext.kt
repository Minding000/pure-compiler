package compiler.targets.llvm

import errors.internal.CompilerError
import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

class BuildContext(name: String) {
	val context: LLVMContextRef = LLVM.LLVMContextCreate()
	val module = LLVM.LLVMModuleCreateWithNameInContext(name, context)
	val builder = LLVM.LLVMCreateBuilderInContext(context)
	val i32Type = LLVM.LLVMInt32TypeInContext(context)

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