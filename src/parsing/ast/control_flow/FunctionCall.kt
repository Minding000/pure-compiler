package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.FunctionCall
import linter.elements.general.Unit
import linter.scopes.Scope
import source_structure.Position
import parsing.ast.general.Element
import util.concretize
import util.indent
import util.toLines

class FunctionCall(private val functionReference: Element, private val parameters: List<Element>, end: Position): Element(functionReference.start, end) {

	override fun concretize(linter: Linter, scope: Scope): FunctionCall {
		return FunctionCall(this, functionReference.concretize(linter, scope), parameters.concretize(linter, scope))
	}

	override fun toString(): String {
		return "FunctionCall [ $functionReference ] {${parameters.toLines().indent()}\n}"
	}
}