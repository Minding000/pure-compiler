package compiler.value_analysis

import compiler.instructions.Instruction

open class ValueReference(var instruction: Instruction, val type: ValueReferenceType)