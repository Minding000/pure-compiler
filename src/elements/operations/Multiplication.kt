package elements.operations

import code.InstructionGenerator
import instructions.Mul
import objects.Element
import objects.Register

class Multiplication(val left: Element, val right: Element, val isDivision: Boolean): Element() {

    override fun generateInstructions(generator: InstructionGenerator): Register {
        val register = generator.createRegister()
        val leftRegister = left.generateInstructions(generator)
        val rightRegister = right.generateInstructions(generator)
        generator.instructions.add(Mul(register, leftRegister, rightRegister, isDivision))
        return register
    }

    override fun toString(): String {
        return "Multiplication { $left ${ if(isDivision) "/" else "*" } $right }"
    }
}