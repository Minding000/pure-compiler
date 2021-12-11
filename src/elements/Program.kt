package elements

import code.InstructionGenerator
import code.Main
import value_analysis.DynamicValue
import java.lang.StringBuilder

class Program(val statements: List<Element>): VoidElement(statements.first().start, statements.last().end) {

    override fun generateInstructions(generator: InstructionGenerator): DynamicValue? {
        for(statement in statements)
            statement.generateInstructions(generator)
        return null
    }

    override fun toString(): String {
        val string = StringBuilder()
        for(statement in statements)
            string.append("\n").append(statement.toString())
        return "Program {${Main.indentText(string.toString())}\n}"
    }
}