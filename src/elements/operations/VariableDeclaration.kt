package elements.operations

import code.InstructionGenerator
import code.Main
import elements.identifier.VariableIdentifier
import value_analysis.DynamicValue
import elements.Element
import elements.VoidElement
import source_structure.Position
import java.lang.StringBuilder

class VariableDeclaration(start: Position, end: Position, val elements: List<Element>): VoidElement(start, end) {

    override fun generateInstructions(generator: InstructionGenerator): DynamicValue? {
        for(element in elements) {
            if(element is VariableIdentifier)
                element.getNewDynamicValue(generator)
            else
                element.generateInstructions(generator)
        }
        return null
    }

    override fun toString(): String {
        val string = StringBuilder()
        for(element in elements)
            string.append("\n").append(element.toString())
        return "Declaration {${Main.indentText(string.toString())}\n}"
    }
}