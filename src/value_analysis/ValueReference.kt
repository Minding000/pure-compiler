package value_analysis

import instructions.Instruction

open class ValueReference(var instruction: Instruction, val type: ValueReferenceType)