package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.FunctionCall
import linter.scopes.MutableScope
import source_structure.Position
import parsing.ast.general.ValueElement
import util.concretizeValues
import util.indent
import util.toLines

class FunctionCall(private val functionReference: ValueElement, private val parameters: List<ValueElement>,
				   end: Position): ValueElement(functionReference.start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): FunctionCall {
		return FunctionCall(this, functionReference.concretize(linter, scope),
			parameters.concretizeValues(linter, scope))
	}

	override fun toString(): String {
		return "FunctionCall [ $functionReference ] {${parameters.toLines().indent()}\n}"
	}
}