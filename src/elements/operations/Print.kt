package elements.operations

import code.InstructionGenerator
import code.Main
import elements.Identifier
import instructions.Prt
import objects.Element
import objects.Register
import objects.ValueSource
import java.lang.StringBuilder
import java.util.*

// NOTE: This instruction is only for development purposes
class Print(val identifiers: List<Identifier>): Element() {

	override fun generateInstructions(generator: InstructionGenerator): Register {
		val valueSources = LinkedList<ValueSource>()
		for(identifier in identifiers)
			valueSources.add(identifier.generateInstructions(generator))
		generator.instructions.add(Prt(valueSources))
		return generator.voidRegister
	}

	override fun toString(): String {
		val string = StringBuilder()
		for (identifier in identifiers)
			string.append("\n").append(identifier.toString())
		return "Print {${Main.indentText(string.toString())}\n}"
	}
}