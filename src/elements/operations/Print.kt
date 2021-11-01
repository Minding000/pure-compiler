package elements.operations

import code.InstructionGenerator
import code.Main
import elements.Identifier
import instructions.Prt
import objects.Element
import objects.Register
import java.lang.StringBuilder
import java.util.*

// NOTE: This instruction is only for development purposes
class Print(val identifiers: List<Identifier>): Element() {

	override fun generateInstructions(generator: InstructionGenerator): Register {
		val registers = LinkedList<Register>()
		for(identifier in identifiers)
			registers.add(identifier.generateInstructions(generator))
		generator.instructions.add(Prt(registers))
		return generator.voidRegister
	}

	override fun toString(): String {
		val string = StringBuilder()
		for (identifier in identifiers)
			string.append("\n").append(identifier.toString())
		return "Print {${Main.indentText(string.toString())}\n}"
	}
}