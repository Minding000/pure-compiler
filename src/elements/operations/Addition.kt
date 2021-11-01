package elements.operations

import code.InstructionGenerator
import instructions.Add
import objects.Element
import objects.Register

class Addition(val left: Element, val right: Element, val isNegative: Boolean): Element() {

    override fun generateInstructions(generator: InstructionGenerator): Register {
        val register = generator.createRegister()
        val leftRegister = left.generateInstructions(generator)
        val rightRegister = right.generateInstructions(generator)
        generator.instructions.add(Add(register, leftRegister, rightRegister, isNegative))
        return register
    }

    override fun toString(): String {
        return "Addition { $left ${ if(isNegative) "-" else "+" } $right }"
    }
}