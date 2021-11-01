package elements.operations

import code.InstructionGenerator
import code.Main
import objects.Element
import objects.Register
import java.lang.StringBuilder

class Declaration(val elements: List<Element>): Element() {

    override fun generateInstructions(generator: InstructionGenerator): Register {
        for(element in elements)
            element.generateInstructions(generator)
        return generator.voidRegister
    }

    override fun toString(): String {
        val string = StringBuilder()
        for (element in elements)
            string.append("\n").append(element.toString())
        return "Declaration {${Main.indentText(string.toString())}\n}"
    }
}