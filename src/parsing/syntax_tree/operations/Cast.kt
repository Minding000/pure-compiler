package parsing.syntax_tree.operations

import errors.internal.CompilerError
import linting.Linter
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.operations.Cast
import parsing.syntax_tree.general.ValueElement
import parsing.syntax_tree.literals.Identifier
import parsing.syntax_tree.general.TypeElement
import util.indent

class Cast(val value: ValueElement, val operator: String, val identifier: Identifier?, val type: TypeElement):
	ValueElement(value.start, type.end) {

	override fun concretize(linter: Linter, scope: MutableScope): Cast {
		val operator = Cast.Operator.values().find { castType -> castType.stringRepresentation == operator }
			?: throw CompilerError("Unknown cast operator '$operator'.")
		return Cast(this, value.concretize(linter, scope), identifier?.concretize(linter, scope),
			type.concretize(linter, scope), operator)
	}

	override fun toString(): String {
		return "Cast {${"\n$value $operator ${if(identifier == null) "" else "$identifier: "}$type".indent()}\n}"
	}
}