package util

import components.code_generation.llvm.wrapper.LlvmList
import errors.internal.CompilerError
import org.bytedeco.javacpp.Pointer

inline fun <reified T: Pointer> List<T?>.toLlvmList(): LlvmList<T> {
	val llvmList = LlvmList<T>(size.toLong())
	for((index, pointer) in withIndex())
		llvmList.put(index.toLong(), pointer ?: throw CompilerError("LLVM list contains null value."))
	return llvmList
}
