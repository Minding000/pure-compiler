package util

import components.code_generation.llvm.LlvmList
import org.bytedeco.javacpp.Pointer

inline fun <reified T: Pointer?> List<T>.toLlvmList(): LlvmList<T> = LlvmList(*toTypedArray())
