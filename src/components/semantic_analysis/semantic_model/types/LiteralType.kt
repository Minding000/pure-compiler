package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.resolution.NotFound

class LiteralType(override val source: Element, scope: Scope, val literalType: Linter.SpecialType):
	ObjectType(source, scope, literalType.className) {

	constructor(source: Element, scope: Scope, literalType: Linter.SpecialType, linter: Linter): this(source, scope, literalType) {
		linkTypes(linter)
	}

	override fun linkTypes(linter: Linter) {
		definition = literalType.scope?.resolveType(name)
		if(definition == null)
			linter.addIssue(NotFound(source, "Literal type", name, true))
	}

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): Type {
		return this
	}
}
