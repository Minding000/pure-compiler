package objects

import code.InstructionGenerator

abstract class Element() {

    open fun generateInstructions(generator: InstructionGenerator): Register {
        return generator.voidRegister
    }

    override fun toString(): String {
        return "Element { --- }"
    }
}