package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.resolution.LiteralTypeNotFound

class LiteralType(override val source: Element, scope: Scope, val literalType: SpecialType):
	ObjectType(source, scope, literalType.className) {

	override fun resolveDefinitions() {
		definition = literalType.scope?.resolveType(name)
		definition?.scope?.subscribe(this)
		if(definition == null)
			context.addIssue(LiteralTypeNotFound(source, name))
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Type {
		return this
	}
}
