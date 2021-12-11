package elements.operations

import code.InstructionGenerator
import code.Main
import elements.ValueElement
import instructions.Prt
import value_analysis.DynamicValue
import elements.VoidElement
import source_structure.Position
import value_analysis.ValueSource
import java.lang.StringBuilder
import java.util.*

// NOTE: This instruction is only for development purposes
class Print(start: Position, end: Position, val elements: List<ValueElement>): VoidElement(start, end) {

	override fun generateInstructions(generator: InstructionGenerator): DynamicValue? {
		val valueSources = LinkedList<ValueSource>()
		for(element in elements)
			valueSources.add(element.generateInstructions(generator))
		generator.instructions.add(Prt(valueSources))
		return null
	}

	override fun toString(): String {
		val string = StringBuilder()
		for(element in elements)
			string.append("\n").append(element.toString())
		return "Print {${Main.indentText(string.toString())}\n}"
	}
}