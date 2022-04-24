package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.VariableDeclaration
import linter.scopes.Scope
import parsing.ast.definitions.sections.VariableSectionElement
import parsing.ast.general.Element
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type
import java.lang.StringBuilder

class VariableDeclaration(private val identifier: Identifier, private val type: Type?, private val value: Element?):
	VariableSectionElement(identifier.start, (value ?: type ?: identifier).end) {

	override fun concretize(linter: Linter, scope: Scope): VariableDeclaration {
		//TODO include modifiers
		val variableDeclaration = VariableDeclaration(
			this,
			identifier.getValue(),
			type?.concretize(linter, scope),
			value?.concretize(linter, scope),
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
