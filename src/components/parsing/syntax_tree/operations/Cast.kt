package components.parsing.syntax_tree.operations

import errors.internal.CompilerError
import components.linting.Linter
import components.linting.semantic_model.scopes.MutableScope
import components.linting.semantic_model.operations.Cast as SemanticCastModel
import components.parsing.syntax_tree.general.ValueElement
import components.parsing.syntax_tree.literals.Identifier
import components.parsing.syntax_tree.general.TypeElement
import util.indent

class Cast(val value: ValueElement, val operator: String, val identifier: Identifier?, val type: TypeElement):
	ValueElement(value.start, type.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticCastModel {
		val operator = SemanticCastModel.Operator.values().find { castType ->
			castType.stringRepresentation == operator }
			?: throw CompilerError("Unknown cast operator '$operator'.")
		return SemanticCastModel(this, value.concretize(linter, scope), identifier?.concretize(linter, scope),
			type.concretize(linter, scope), operator)
	}

	override fun toString(): String {
		return "Cast {${"\n$value $operator ${if(identifier == null) "" else "$identifier: "}$type".indent()}\n}"
	}
}
