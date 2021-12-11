package elements.operations

import code.InstructionGenerator
import instructions.Mul
import value_analysis.DynamicValue
import elements.ValueElement

class Multiplication(val left: ValueElement, val right: ValueElement, val isDivision: Boolean): ValueElement(left.start, right.end, left.type) {

    override fun generateInstructions(generator: InstructionGenerator): DynamicValue {
        val multiplicationInstruction = Mul(
            left.generateInstructions(generator),
            right.generateInstructions(generator),
            isDivision
        )
        generator.instructions.add(multiplicationInstruction)
        return multiplicationInstruction.output
    }

    override fun toString(): String {
        return "Multiplication { $left ${ if(isDivision) "/" else "*" } $right }"
    }
}