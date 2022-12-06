package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration as SemanticLocalVariableDeclarationModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.VariableSectionElement
import components.syntax_parser.syntax_tree.literals.Identifier
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.general.ValueElement
import components.tokenizer.WordAtom
import java.lang.StringBuilder

class LocalVariableDeclaration(private val identifier: Identifier, private val type: TypeElement?,
							   private val value: ValueElement?):
	VariableSectionElement(identifier.start, (value ?: type ?: identifier).end) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.IMMUTABLE)
	}

	override fun concretize(linter: Linter, scope: MutableScope): SemanticLocalVariableDeclarationModel {
		parent.validate(linter, ALLOWED_MODIFIER_TYPES)
		val isMutable = !parent.containsModifier(WordAtom.IMMUTABLE)
		val variableDeclaration = SemanticLocalVariableDeclarationModel(
			this,
			identifier.getValue(),
			(type ?: parent.type)?.concretize(linter, scope),
			(value ?: parent.value)?.concretize(linter, scope),
			parent.isConstant,
			isMutable
		)
		scope.declareValue(linter, variableDeclaration)
		return variableDeclaration
	}

	override fun toString(): String {
		val string = StringBuilder()
		string
			.append("LocalVariableDeclaration { ")
			.append(identifier.toString())
		if(type != null)
			string.append(": ").append(type)
		if(value != null)
			string.append(" = ").append(value)
		string.append(" }")
		return string.toString()
	}
}
