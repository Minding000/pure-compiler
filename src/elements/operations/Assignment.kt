package elements.operations

import code.InstructionGenerator
import elements.Identifier
import instructions.Copy
import objects.Element
import objects.Register

class Assignment(val identifier: Identifier, val value: Element): Element() {

    override fun generateInstructions(generator: InstructionGenerator): Register {
        val targetRegister = identifier.generateInstructions(generator)
        val sourceRegister = value.generateInstructions(generator)
        generator.instructions.add(Copy(targetRegister, sourceRegister))
        return generator.voidRegister
    }

    override fun toString(): String {
        return "Assignment { $identifier = $value }"
    }
}