package parsing.ast.definitions

import errors.internal.CompilerError
import linter.Linter
import linter.elements.definitions.ComputedProperty
import linter.scopes.Scope
import parsing.ast.definitions.sections.VariableSection
import parsing.ast.general.Element
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type
import util.indent
import util.toLines
import java.lang.StringBuilder

class ComputedProperty(private val identifier: Identifier, private val type: Type?, private val getExpression: Element?,
					   private val setExpression: Element?):
	Element(identifier.start, setExpression?.end ?: getExpression?.end ?: identifier.end) {
	lateinit var parent: VariableSection

	override fun concretize(linter: Linter, scope: Scope): ComputedProperty {
		val type = type ?: parent.type ?: throw CompilerError("Computed property is missing type. [should be linker error instead]")
		return ComputedProperty(this, identifier.getValue(), type.concretize(linter, scope),
			getExpression?.concretize(linter, scope), setExpression?.concretize(linter, scope))
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