package components.semantic_model.values

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.LlvmValue
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.operations.IndexAccess
import components.semantic_model.operations.MemberAccess
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.Type
import logger.issues.resolution.*
import components.syntax_parser.syntax_tree.literals.SuperReference as SuperReferenceSyntaxTree

open class SuperReference(override val source: SuperReferenceSyntaxTree, scope: Scope, private val specifier: ObjectType?):
	Value(source, scope) {

	init {
		addSemanticModels(specifier)
	}

	override fun determineTypes() {
		super.determineTypes()
		val surroundingTypeDeclaration = scope.getSurroundingTypeDeclaration()
		if(surroundingTypeDeclaration == null) {
			context.addIssue(SuperReferenceOutsideOfTypeDefinition(source))
			return
		}
		val superTypes = if(specifier == null) {
			surroundingTypeDeclaration.getDirectSuperTypes()
		} else {
			val specifierDefinition = specifier.getTypeDeclaration()
			if(specifierDefinition == null) {
				emptyList()
			} else {
				val superType = surroundingTypeDeclaration.getAllSuperTypes().find { superType ->
					matchesSpecifier(superType, specifierDefinition) }
				if(superType == null) {
					context.addIssue(SuperReferenceSpecifierNotInherited(source, surroundingTypeDeclaration, specifier))
					return
				}
				listOf(superType)
			}
		}
		val possibleTargetTypes = when(val parent = parent) {
			is MemberAccess -> {
				if(parent.member is InitializerReference) {
					if(!isInInitializer()) {
						context.addIssue(SuperInitializerCallOutsideOfInitializer(source))
						return
					}
				}
				parent.filterForPossibleTargetTypes(superTypes)
			}
			is IndexAccess -> {
				parent.filterForPossibleTargetTypes(superTypes)
			}
			else -> {
				context.addIssue(SuperReferenceOutsideOfAccess(source))
				return
			}
		}
		if(possibleTargetTypes.isEmpty()) {
			context.addIssue(SuperMemberNotFound(source))
		} else if(possibleTargetTypes.size > 1) {
			context.addIssue(SuperReferenceAmbiguity(source, possibleTargetTypes))
		} else {
			val intendedType = possibleTargetTypes.first()
			type = intendedType
		}
	}

	private fun matchesSpecifier(superType: Type, specifierDefinition: TypeDeclaration): Boolean {
		if(superType !is ObjectType)
			return false
		if(superType.getTypeDeclaration() == specifierDefinition)
			return true
		return false
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		return context.getThisParameter(constructor)
	}
}
