package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.VariableSectionElement
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.general.ValueElement
import components.syntax_parser.syntax_tree.literals.Identifier
import components.tokenizer.WordAtom
import util.indent
import components.semantic_analysis.semantic_model.definitions.ComputedPropertyDeclaration as SemanticComputedPropertyDeclarationModel

class ComputedPropertyDeclaration(private val identifier: Identifier, private val type: TypeElement?,
								  private val getExpression: ValueElement?, private val setStatement: Element?):
	VariableSectionElement(identifier.start, setStatement?.end ?: getExpression?.end ?: identifier.end) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.OVERRIDING)
	}

	override fun concretize(linter: Linter, scope: MutableScope): SemanticComputedPropertyDeclarationModel {
		parent.validate(linter, ALLOWED_MODIFIER_TYPES)
		val isOverriding = parent.containsModifier(WordAtom.OVERRIDING)
		val type = type ?: parent.type
		val computedProperty = SemanticComputedPropertyDeclarationModel(this, scope, identifier.getValue(),
			type?.concretize(linter, scope), parent.isConstant, isOverriding, getExpression?.concretize(linter, scope),
			setStatement?.concretize(linter, scope))
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
		if(setStatement != null)
			string.append("\n\tsets ").append(setStatement.toString().indent())
		string.append("\n}")
		return string.toString()
	}
}
