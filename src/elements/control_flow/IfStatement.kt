package elements.control_flow

import code.InstructionGenerator
import code.Main
import elements.Element
import elements.ValueElement
import value_analysis.DynamicValue
import elements.VoidElement
import source_structure.Position

class IfStatement(val condition: ValueElement, val trueBranch: Element, val falseBranch: Element?, start: Position, end: Position): VoidElement(start, end) {

	override fun generateInstructions(generator: InstructionGenerator): DynamicValue? {
		//TODO actually generate instructions for the if statement
		val conditionValue = condition.generateInstructions(generator)
		trueBranch.generateInstructions(generator)
		falseBranch?.generateInstructions(generator)
		return null
	}

	override fun toString(): String {
		return "If {${Main.indentText("\n${trueBranch}")}\n}${if(falseBranch == null) "" else " Else {${Main.indentText("\n${falseBranch}")}\n}"}"
	}
}