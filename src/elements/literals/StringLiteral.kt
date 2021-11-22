package elements.literals

import code.InstructionGenerator
import instructions.Init
import objects.Element
import objects.Register
import objects.Value

class StringLiteral(val value: String): Element() {

    override fun generateInstructions(generator: InstructionGenerator): Register {
        val register = generator.createRegister()
        generator.instructions.add(Init(register, Value(value)))
        return register
    }

    override fun toString(): String {
        return "StringLiteral { \"$value\" }"
    }
}