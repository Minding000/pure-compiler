package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.values.VariableValueDeclaration
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.definitions.sections.VariableSectionElement
import parsing.syntax_tree.literals.Identifier
import parsing.syntax_tree.general.TypeElement
import parsing.syntax_tree.general.ValueElement
import java.lang.StringBuilder

class VariableDeclaration(private val identifier: Identifier, private val type: TypeElement?,
						  private val value: ValueElement?):
	VariableSectionElement(identifier.start, (value ?: type ?: identifier).end) {

	override fun concretize(linter: Linter, scope: MutableScope): VariableValueDeclaration {
		val variableDeclaration = VariableValueDeclaration(
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
