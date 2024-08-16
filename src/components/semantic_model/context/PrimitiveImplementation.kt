package components.semantic_model.context

import components.code_generation.llvm.wrapper.LlvmType
import components.code_generation.llvm.wrapper.LlvmValue

class PrimitiveImplementation(val llvmValue: LlvmValue, val llvmType: LlvmType)
