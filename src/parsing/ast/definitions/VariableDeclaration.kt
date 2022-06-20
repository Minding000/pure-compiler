package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.VariableDeclaration
import linter.scopes.MutableScope
import parsing.ast.definitions.sections.VariableSectionElement
import parsing.ast.literals.Identifier
import parsing.ast.general.TypeElement
import parsing.ast.general.ValueElement
import java.lang.StringBuilder

class VariableDeclaration(private val identifier: Identifier, private val type: TypeElement?,
						  private val value: ValueElement?):
	VariableSectionElement(identifier.start, (value ?: type ?: identifier).end) {

	override fun concretize(linter: Linter, scope: MutableScope): VariableDeclaration {
		val variableDeclaration = VariableDeclaration(
			this,
			identifier.getValue(),
			(type ?: parent.type)?.concretize(linter, scope),
			(value ?: parent.value)?.concretize(linter, scope),
			parent.isConstant
		)
		scope.declareValue(linter, variableDeclaration)
		return variableDeclaration
	}

	override fun toString(): String {
		val string = StringBuilder()
		string
			.append("VariableDeclaration { ")
			.append(identifier.toString())
		if(type != null)
			string.append(": ").append(type)
		if(value != null)
			string.append(" = ").append(value)
		string.append(" }")
		return string.toString()
	}
}
