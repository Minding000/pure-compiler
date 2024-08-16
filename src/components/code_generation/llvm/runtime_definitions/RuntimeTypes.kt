package components.code_generation.llvm.runtime_definitions

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmType

class RuntimeTypes {
	lateinit var memberIndex: LlvmType
	lateinit var memberId: LlvmType
	lateinit var memberOffset: LlvmType
	lateinit var memberAddress: LlvmType

	fun declare(constructor: LlvmConstructor) {
		memberIndex = constructor.i32Type
		memberId = constructor.i32Type
		memberOffset = constructor.i32Type
		memberAddress = constructor.pointerType
	}
}
