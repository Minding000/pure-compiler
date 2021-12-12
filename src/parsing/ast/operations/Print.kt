package parsing.ast.operations

import compiler.InstructionGenerator
import code.Main
import compiler.instructions.Prt
import compiler.value_analysis.DynamicValue
import source_structure.Position
import compiler.value_analysis.ValueSource
import parsing.ast.Element
import java.lang.StringBuilder
import java.util.*

// NOTE: This instruction is only for development purposes
class Print(start: Position, end: Position, val elements: List<Element>): Element(start, end) {

	override fun toString(): String {
		val string = StringBuilder()
		for(element in elements)
			string.append("\n").append(element.toString())
		return "Print {${Main.indentText(string.toString())}\n}"
	}
}