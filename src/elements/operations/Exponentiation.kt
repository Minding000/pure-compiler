package elements.operations

import code.InstructionGenerator
import instructions.Mul
import value_analysis.DynamicValue
import elements.ValueElement
import instructions.Exp

class Exponentiation(val left: ValueElement, val right: ValueElement): ValueElement(left.start, right.end, left.type) {

    override fun generateInstructions(generator: InstructionGenerator): DynamicValue {
        val exponentiationInstruction = Exp(
            left.generateInstructions(generator),
            right.generateInstructions(generator)
        )
        generator.instructions.add(exponentiationInstruction)
        return exponentiationInstruction.output
    }

    override fun toString(): String {
        return "Exponentiation { $left ^ $right }"
    }
}