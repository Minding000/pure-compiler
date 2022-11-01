package components.compiler.value_analysis

import components.compiler.instructions.Instruction

open class ValueReference(var instruction: Instruction, val type: ValueReferenceType)
