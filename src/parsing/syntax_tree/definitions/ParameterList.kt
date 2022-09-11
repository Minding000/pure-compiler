package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.definitions.Parameter as SemanticParameterModel
import parsing.syntax_tree.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines
import java.util.*

class ParameterList(start: Position, end: Position, val parameters: List<Parameter>): MetaElement(start, end) {

	fun concretizeParameters(linter: Linter, scope: MutableScope): List<SemanticParameterModel> {
		val parameters = LinkedList<SemanticParameterModel>()
		for(parameter in this.parameters)
			parameters.add(parameter.concretize(linter, scope))
		return parameters
	}

	override fun toString(): String {
		return "ParameterList {${parameters.toLines().indent()}\n}"
	}
}