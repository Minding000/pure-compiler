package elements.control_flow

import code.InstructionGenerator
import code.Main
import elements.ValueElement
import elements.identifier.IdentifierReference
import elements.identifier.VariableIdentifier
import source_structure.Position
import types.Type
import value_analysis.DynamicValue
import java.lang.StringBuilder

class FunctionCall(val functionReference: IdentifierReference<VariableIdentifier>, val parameters: List<ValueElement>, start: Position, end: Position): ValueElement(start, end) {

	override fun generateInstructions(generator: InstructionGenerator): DynamicValue {
		//TODO actually generate instructions for the function call
		return DynamicValue()
	}

	override fun toString(): String {
		val string = StringBuilder()
		for(parameter in parameters)
			string.append("\n").append(parameter.toString())
		return "FunctionCall [${functionReference.getValue()}] {${Main.indentText(string.toString())}\n}"
	}
}