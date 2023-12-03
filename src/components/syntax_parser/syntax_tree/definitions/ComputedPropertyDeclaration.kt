package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.scopes.BlockScope
import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.ComputedPropertySection
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import components.tokenizer.WordAtom
import util.indent
import components.semantic_model.declarations.ComputedPropertyDeclaration as SemanticComputedPropertyDeclarationModel

class ComputedPropertyDeclaration(private val identifier: Identifier, private val type: TypeSyntaxTreeNode?,
								  private val whereClause: WhereClause?, private val getter: SyntaxTreeNode?,
								  private val setter: SyntaxTreeNode?):
	SyntaxTreeNode(identifier.start, setter?.end ?: getter?.end ?: identifier.end) {
	lateinit var parent: ComputedPropertySection

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.ABSTRACT, WordAtom.OVERRIDING)
	}

	override fun toSemanticModel(scope: MutableScope): SemanticComputedPropertyDeclarationModel {
		parent.validate(ALLOWED_MODIFIER_TYPES)
		val isAbstract = parent.containsModifier(WordAtom.ABSTRACT)
		val isOverriding = parent.containsModifier(WordAtom.OVERRIDING)
		val type = type ?: parent.type
		val whereClauseConditions = whereClause?.toWhereClauseConditionSemanticModels(scope) ?: emptyList()
		val getterScope = BlockScope(scope)
		val setterScope = BlockScope(scope)
		return SemanticComputedPropertyDeclarationModel(this, scope, identifier.getValue(), type?.toSemanticModel(scope),
			whereClauseConditions, isOverriding, isAbstract, getterScope, setterScope, getter?.toSemanticModel(getterScope),
			setter?.toSemanticModel(setterScope))
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
		if(getter != null)
			stringRepresentation.append("\n\tgets ").append(getter.toString().indent())
		if(setter != null)
			stringRepresentation.append("\n\tsets ").append(setter.toString().indent())
		stringRepresentation.append("\n}")
		return stringRepresentation.toString()
	}
}
