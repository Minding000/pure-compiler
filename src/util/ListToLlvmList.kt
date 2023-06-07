package util

import components.compiler.targets.llvm.LlvmList
import org.bytedeco.javacpp.Pointer

fun <T: Pointer?> List<T>.toLlvmList(): LlvmList<T> {
	val llvmList = LlvmList<T>(size.toLong())
	for(element in this)
		llvmList.put(element)
	return llvmList
}
