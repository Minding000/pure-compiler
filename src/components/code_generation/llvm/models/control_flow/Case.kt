package components.code_generation.llvm.models.control_flow

import components.code_generation.llvm.models.general.ErrorHandlingContext
import components.code_generation.llvm.models.general.Unit
import components.code_generation.llvm.models.values.Value
import components.semantic_model.control_flow.Case

class Case(override val model: Case, val condition: Value, val result: ErrorHandlingContext): Unit(model, listOf(condition, result))
