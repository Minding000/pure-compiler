package components.semantic_model.values

import components.semantic_model.operations.MemberAccess
import components.semantic_model.scopes.InterfaceScope
import components.semantic_model.scopes.Scope
import components.semantic_model.types.StaticType
import logger.issues.resolution.InitializerReferenceOutsideOfInitializer
import components.syntax_parser.syntax_tree.literals.InitializerReference as InitializerReferenceSyntaxTree

open class InitializerReference(override val source: InitializerReferenceSyntaxTree, scope: Scope): Value(source, scope) {

	override fun determineTypes() {
		super.determineTypes()
		val scope = scope
		if(scope is InterfaceScope && scope.type is StaticType) {
			providedType = scope.type
		} else {
			val parent = parent
			val surroundingDefinition = if(parent is MemberAccess && parent.target is SelfReference)
				parent.target.typeDeclaration
			else
				scope.getSurroundingTypeDeclaration()
			if(surroundingDefinition == null || !isInInitializer()) {
				context.addIssue(InitializerReferenceOutsideOfInitializer(source))
			} else {
				providedType = StaticType(surroundingDefinition)
				providedType?.determineTypes()
				addSemanticModels(providedType)
			}
		}
	}
}
