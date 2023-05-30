package components.compiler.targets.python.value_analysis

import components.compiler.targets.python.instructions.Instruction

open class ValueReference(var instruction: Instruction, val type: ValueReferenceType)
