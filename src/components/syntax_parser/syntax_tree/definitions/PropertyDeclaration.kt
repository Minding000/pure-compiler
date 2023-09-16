package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.VariableSectionSyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import components.tokenizer.WordAtom
import components.semantic_model.declarations.PropertyDeclaration as SemanticPropertyDeclarationModel

class PropertyDeclaration(private val identifier: Identifier, private val type: TypeSyntaxTreeNode?, private val value: ValueSyntaxTreeNode?):
	VariableSectionSyntaxTreeNode(identifier.start, (value ?: type ?: identifier).end) {

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.ABSTRACT, WordAtom.IMMUTABLE, WordAtom.OVERRIDING)
	}

	override fun toSemanticModel(scope: MutableScope): SemanticPropertyDeclarationModel {
		parent.validate(ALLOWED_MODIFIER_TYPES)
		val isAbstract = parent.containsModifier(WordAtom.ABSTRACT)
		val isMutable = !parent.containsModifier(WordAtom.IMMUTABLE)
		val isOverriding = parent.containsModifier(WordAtom.OVERRIDING)
		val type = (type ?: parent.type)?.toSemanticModel(scope)
		val value = (value ?: parent.value)?.toSemanticModel(scope)
		//TODO 'isStatic' should be 'true' for 'const'
		return SemanticPropertyDeclarationModel(this, scope, identifier.getValue(), type, value, false, isAbstract,
			parent.isConstant, isMutable, isOverriding)
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
