package components.code_generation.python.value_analysis

import components.code_generation.python.instructions.Instruction

open class ValueReference(var instruction: Instruction, val type: ValueReferenceType)
