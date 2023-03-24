package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.operations.IndexAccess
import components.semantic_analysis.semantic_model.operations.MemberAccess
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import logger.issues.resolution.*
import components.syntax_parser.syntax_tree.literals.SuperReference as SuperReferenceSyntaxTree

open class SuperReference(override val source: SuperReferenceSyntaxTree, scope: Scope, private val specifier: ObjectType?):
	Value(source, scope) {

	init {
		addUnits(specifier)
	}

	override fun linkValues(linter: Linter) {
		val surroundingDefinition = scope.getSurroundingDefinition()
		if(surroundingDefinition == null) {
			linter.addIssue(SuperReferenceOutsideOfTypeDefinition(source))
			return
		}
		var superTypes = surroundingDefinition.getAllSuperTypes()
		specifier?.definition?.let { specifierDefinition ->
			superTypes = superTypes.filter { superType -> matchesSpecifier(superType, specifierDefinition) }
			if(superTypes.isEmpty()) {
				linter.addIssue(SuperReferenceSpecifierNotInherited(source, surroundingDefinition, specifier))
				return
			}
		}
		val possibleTargetTypes = when(val parent = parent) {
			is MemberAccess -> {
				if(parent.member is InitializerReference) {
					if(!isInInitializer()) {
						linter.addIssue(SuperInitializerCallOutsideOfInitializer(source))
						return
					}
				}
				parent.filterForPossibleTargetTypes(linter, superTypes)
			}
			is IndexAccess -> {
				parent.filterForPossibleTargetTypes(linter, superTypes)
			}
			else -> {
				linter.addIssue(SuperReferenceOutsideOfAccess(source))
				return
			}
		}
		if(possibleTargetTypes.isEmpty()) {
			linter.addIssue(SuperMemberNotFound(source))
		} else if(possibleTargetTypes.size > 1) {
			linter.addIssue(SuperReferenceAmbiguity(source, possibleTargetTypes))
		} else {
			val intendedType = possibleTargetTypes.first()
			type = intendedType
		}
	}

	private fun matchesSpecifier(superType: Type, specifierDefinition: TypeDefinition): Boolean {
		if(superType !is ObjectType)
			return false
		if(superType.definition == specifierDefinition)
			return true
		if(superType.definition?.baseDefinition == specifierDefinition)
			return true
		return false
	}
}
