package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.resolution.LiteralTypeNotFound

class LiteralType(override val source: Element, scope: Scope, val literalType: Linter.SpecialType):
	ObjectType(source, scope, literalType.className) {

	override fun resolveDefinitions(linter: Linter) {
		definition = literalType.scope?.resolveType(name)
		definition?.scope?.subscribe(this)
		if(definition == null)
			linter.addIssue(LiteralTypeNotFound(source, name))
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): Type {
		return this
	}
}
