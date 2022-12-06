package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.VariableSectionElement
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.general.ValueElement
import components.syntax_parser.syntax_tree.literals.Identifier
import util.indent
import components.semantic_analysis.semantic_model.definitions.ComputedPropertyDeclaration as SemanticComputedPropertyDeclarationModel

class ComputedPropertyDeclaration(private val identifier: Identifier, private val type: TypeElement?,
								  private val getExpression: ValueElement?, private val setExpression: ValueElement?):
	VariableSectionElement(identifier.start, setExpression?.end ?: getExpression?.end ?: identifier.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticComputedPropertyDeclarationModel {
		val type = type ?: parent.type
		val computedProperty = SemanticComputedPropertyDeclarationModel(this, identifier.getValue(),
			type?.concretize(linter, scope), getExpression?.concretize(linter, scope),
			setExpression?.concretize(linter, scope))
		scope.declareValue(linter, computedProperty)
		return computedProperty
	}

	override fun toString(): String {
		val string = StringBuilder()
		string
			.append("ComputedPropertyDeclaration {\n\t")
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
