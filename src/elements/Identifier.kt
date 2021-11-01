package elements

import code.InstructionGenerator
import objects.Element
import objects.Register

class Identifier(val name: String): Element() {

    override fun generateInstructions(generator: InstructionGenerator): Register {
        var register = generator.namedRegisters[name]
        if(register == null) {
            register = generator.createRegister()
            generator.namedRegisters[name] = register
            return register
        }
        return register
    }

    override fun toString(): String {
        return "Identifier { $name }"
    }
}