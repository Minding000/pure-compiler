package parsing.syntax_tree.definitions

import errors.internal.CompilerError
import linting.Linter
import linting.semantic_model.definitions.ComputedProperty
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.definitions.sections.VariableSectionElement
import parsing.syntax_tree.general.TypeElement
import parsing.syntax_tree.general.ValueElement
import parsing.syntax_tree.literals.Identifier
import util.indent

class ComputedProperty(private val identifier: Identifier, private val type: TypeElement?,
					   private val getExpression: ValueElement?, private val setExpression: ValueElement?):
	VariableSectionElement(identifier.start, setExpression?.end ?: getExpression?.end ?: identifier.end) {

	override fun concretize(linter: Linter, scope: MutableScope): ComputedProperty {
		val type = type ?: parent.type ?: throw CompilerError("Computed property is missing type. [should be linker error instead]") //TODO see error message
		val computedProperty = ComputedProperty(this, identifier.getValue(), type.concretize(linter, scope),
			getExpression?.concretize(linter, scope), setExpression?.concretize(linter, scope))
		scope.declareValue(linter, computedProperty)
		return computedProperty
	}

	override fun toString(): String {
		val string = StringBuilder()
		string
			.append("ComputedProperty {\n\t")
			.append(identifier.toString())
		if(type != null)
			string.append(": ").append(type)
		if(getExpression != null)
			string.append("\n\tgets ").append(getExpression.toString().indent())
		if(setExpression != null)
			string.append("\n\tsets ").append(setExpression)
		string.append("\n}")
		return string.toString()
	}
}
