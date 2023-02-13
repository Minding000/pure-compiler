package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.VariableSectionElement
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.general.ValueElement
import components.syntax_parser.syntax_tree.literals.Identifier
import components.tokenizer.WordAtom
import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration as SemanticPropertyDeclarationModel

class PropertyDeclaration(private val identifier: Identifier, private val type: TypeElement?, private val value: ValueElement?):
	VariableSectionElement(identifier.start, (value ?: type ?: identifier).end) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.ABSTRACT, WordAtom.IMMUTABLE, WordAtom.OVERRIDING)
	}

	override fun concretize(linter: Linter, scope: MutableScope): SemanticPropertyDeclarationModel {
		parent.validate(linter, ALLOWED_MODIFIER_TYPES)
		val isAbstract = parent.containsModifier(WordAtom.ABSTRACT)
		val isMutable = !parent.containsModifier(WordAtom.IMMUTABLE)
		val isOverriding = parent.containsModifier(WordAtom.OVERRIDING)
		val propertyDeclaration = SemanticPropertyDeclarationModel(
			this,
			identifier.getValue(),
			(type ?: parent.type)?.concretize(linter, scope),
			(value ?: parent.value)?.concretize(linter, scope),
			isAbstract,
			parent.isConstant,
			isMutable,
			isOverriding
		)
		scope.declareValue(linter, propertyDeclaration)
		return propertyDeclaration
	}

	override fun toString(): String {
		val string = StringBuilder()
		string
			.append("PropertyDeclaration { ")
			.append(identifier.toString())
		if(type != null)
			string.append(": ").append(type)
		if(value != null)
			string.append(" = ").append(value)
		string.append(" }")
		return string.toString()
	}
}
