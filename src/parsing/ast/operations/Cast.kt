package parsing.ast.operations

import errors.internal.CompilerError
import linter.Linter
import linter.scopes.Scope
import linter.elements.operations.Cast
import parsing.ast.general.ValueElement
import parsing.ast.literals.Identifier
import parsing.ast.general.TypeElement
import util.indent

class Cast(val value: ValueElement, val operator: String, val identifier: Identifier?, val type: TypeElement):
	ValueElement(value.start, type.end) {

	override fun concretize(linter: Linter, scope: Scope): Cast {
		val operator = Cast.Operator.values().find { it.stringRepresentation == operator }
			?: throw CompilerError("Unknown cast operator '$operator'.")
		return Cast(this, value.concretize(linter, scope), identifier?.concretize(linter, scope),
			type.concretize(linter, scope), operator)
	}

	override fun toString(): String {
		return "Cast {${"\n$value $operator ${if(identifier == null) "" else "$identifier: "}$type".indent()}\n}"
	}
}