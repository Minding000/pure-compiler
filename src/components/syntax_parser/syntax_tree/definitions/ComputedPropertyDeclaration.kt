package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.ComputedPropertySection
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.general.ValueSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import components.tokenizer.WordAtom
import util.indent
import components.semantic_model.declarations.ComputedPropertyDeclaration as SemanticComputedPropertyDeclarationModel

class ComputedPropertyDeclaration(private val identifier: Identifier, private val type: TypeSyntaxTreeNode?,
								  private val whereClause: WhereClause?, private val getExpression: ValueSyntaxTreeNode?,
								  private val setStatement: SyntaxTreeNode?):
	SyntaxTreeNode(identifier.start, setStatement?.end ?: getExpression?.end ?: identifier.end) {
	lateinit var parent: ComputedPropertySection

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.ABSTRACT, WordAtom.OVERRIDING)
	}

	override fun toSemanticModel(scope: MutableScope): SemanticComputedPropertyDeclarationModel {
		parent.validate(ALLOWED_MODIFIER_TYPES)
		val isAbstract = parent.containsModifier(WordAtom.ABSTRACT)
		val isOverriding = parent.containsModifier(WordAtom.OVERRIDING)
		val type = type ?: parent.type
		val whereClause = whereClause?.toSemanticModel(scope)
		return SemanticComputedPropertyDeclarationModel(this, scope, identifier.getValue(), type?.toSemanticModel(scope),
			whereClause, isOverriding, isAbstract, getExpression?.toSemanticModel(scope), setStatement?.toSemanticModel(scope))
	}

	override fun toString(): String {
		val stringRepresentation = StringBuilder()
		stringRepresentation
			.append("ComputedPropertyDeclaration {\n\t")
			.append(identifier.toString())
		if(type != null)
			stringRepresentation.append(": ").append(type)
		if(whereClause != null)
			stringRepresentation.append(" $whereClause".indent())
		if(getExpression != null)
			stringRepresentation.append("\n\tgets ").append(getExpression.toString().indent())
		if(setStatement != null)
			stringRepresentation.append("\n\tsets ").append(setStatement.toString().indent())
		stringRepresentation.append("\n}")
		return stringRepresentation.toString()
	}
}
