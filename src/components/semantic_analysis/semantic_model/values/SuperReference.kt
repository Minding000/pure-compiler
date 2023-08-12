package components.semantic_analysis.semantic_model.values

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.definitions.TypeDeclaration
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
		addSemanticModels(specifier)
	}

	override fun determineTypes() {
		super.determineTypes()
		val surroundingTypeDeclaration = scope.getSurroundingTypeDeclaration()
		if(surroundingTypeDeclaration == null) {
			context.addIssue(SuperReferenceOutsideOfTypeDefinition(source))
			return
		}
		var superTypes = surroundingTypeDeclaration.getAllSuperTypes()
		specifier?.typeDeclaration?.let { specifierDefinition ->
			superTypes = superTypes.filter { superType -> matchesSpecifier(superType, specifierDefinition) }
			if(superTypes.isEmpty()) {
				context.addIssue(SuperReferenceSpecifierNotInherited(source, surroundingTypeDeclaration, specifier))
				return
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
		if(superType.typeDeclaration == specifierDefinition)
			return true
		if(superType.typeDeclaration?.baseTypeDeclaration == specifierDefinition)
			return true
		return false
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		return context.getThisParameter(constructor)
	}
}
