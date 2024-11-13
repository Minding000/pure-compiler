package components.code_generation.llvm.models.control_flow

import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.models.values.Value
import components.semantic_model.control_flow.WhileGenerator

class WhileGenerator(override val model: WhileGenerator, val condition: Value): Unit(model, listOf(condition))
