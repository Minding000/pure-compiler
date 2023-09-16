package components.semantic_analysis.semantic_model.values

import components.compiler.targets.llvm.LlvmConstructor
import components.compiler.targets.llvm.LlvmValue
import components.semantic_analysis.semantic_model.declarations.TypeDeclaration
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import logger.issues.resolution.SelfReferenceOutsideOfTypeDefinition
import logger.issues.resolution.SelfReferenceSpecifierNotBound
import components.syntax_parser.syntax_tree.literals.SelfReference as SelfReferenceSyntaxTree

open class SelfReference(override val source: SelfReferenceSyntaxTree, scope: Scope, private val specifier: ObjectType?):
	Value(source, scope) {
	var typeDeclaration: TypeDeclaration? = null

	init {
		addSemanticModels(specifier)
		specifier?.setIsNonSpecificContext()
	}

	override fun determineTypes() {
		super.determineTypes()
		val surroundingDefinition = scope.getSurroundingTypeDeclaration()
		if(surroundingDefinition == null) {
			context.addIssue(SelfReferenceOutsideOfTypeDefinition(source))
			return
		}
		if(specifier == null) {
			typeDeclaration = surroundingDefinition
		} else {
			val specifierDefinition = specifier.getTypeDeclaration()
			if(specifierDefinition != null) {
				if(isBoundTo(surroundingDefinition, specifierDefinition))
					typeDeclaration = specifierDefinition
				else
					context.addIssue(SelfReferenceSpecifierNotBound(source, surroundingDefinition, specifierDefinition))
			}
		}
		typeDeclaration?.let { typeDeclaration ->
			type = ObjectType(typeDeclaration.getGenericTypes(), typeDeclaration)
			type?.determineTypes()
			addSemanticModels(type)
		}
	}

	private fun isBoundTo(childDeclaration: TypeDeclaration, parentDeclaration: TypeDeclaration): Boolean {
		var currentDeclaration = childDeclaration
		while(true) {
			if(currentDeclaration == parentDeclaration)
				return true
			if(!currentDeclaration.isBound)
				break
			currentDeclaration = currentDeclaration.parentTypeDeclaration ?: break
		}
		return false
	}

	override fun createLlvmValue(constructor: LlvmConstructor): LlvmValue {
		return context.getThisParameter(constructor)
	}
}
