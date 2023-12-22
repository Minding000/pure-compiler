package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.VariableSectionSyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import components.tokenizer.WordAtom
import components.semantic_model.declarations.LocalVariableDeclaration as SemanticLocalVariableDeclarationModel

class LocalVariableDeclaration(private val identifier: Identifier, private val type: TypeSyntaxTreeNode?, private val value: ValueSyntaxTreeNode?):
	VariableSectionSyntaxTreeNode(identifier.start, (value ?: type ?: identifier).end) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.IMMUTABLE)
	}

	override fun toSemanticModel(scope: MutableScope): SemanticLocalVariableDeclarationModel {
		parent.validate(ALLOWED_MODIFIER_TYPES)
		val isMutable = !parent.containsModifier(WordAtom.IMMUTABLE)
		val type = (type ?: parent.type)?.toSemanticModel(scope)
		val value = (value ?: parent.value)?.toSemanticModel(scope)
		return SemanticLocalVariableDeclarationModel(this, scope, identifier.getValue(), type, value, parent.isConstant, isMutable)
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
