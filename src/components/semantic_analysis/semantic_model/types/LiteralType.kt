package components.semantic_analysis.semantic_model.types

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.declarations.TypeDeclaration
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.resolution.LiteralTypeNotFound

class LiteralType(override val source: SyntaxTreeNode, scope: Scope, val literalType: SpecialType):
	ObjectType(source, scope, literalType.className) {

	override fun resolveTypeDeclarations() {
		val typeDeclaration = literalType.fileScope?.getTypeDeclaration(name)
		typeDeclarationCache = typeDeclaration
		typeDeclaration?.scope?.addSubscriber(this)
		if(typeDeclaration == null)
			context.addIssue(LiteralTypeNotFound(source, name))
	}

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): Type {
		return this
	}
}
