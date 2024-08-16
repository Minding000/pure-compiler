package components.semantic_model.values

import components.code_generation.llvm.wrapper.LlvmConstructor
import components.code_generation.llvm.wrapper.LlvmValue
import components.semantic_model.context.Context
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.types.ObjectType
import components.semantic_model.types.SelfType
import errors.internal.CompilerError
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
		val typeDeclaration = typeDeclaration
		if(typeDeclaration != null) {
			providedType = SelfType(typeDeclaration)
			providedType?.determineTypes()
			addSemanticModels(providedType)
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

	override fun buildLlvmValue(constructor: LlvmConstructor): LlvmValue {
		var currentValue = context.getThisParameter(constructor)
		if(specifier != null) {
			val specifiedTypeDeclaration = typeDeclaration
				?: throw CompilerError(source, "Self reference is missing a target type declaration.")
			var currentTypeDeclaration = scope.getSurroundingTypeDeclaration()
				?: throw CompilerError(source, "Self reference is missing a surrounding type declaration.")
			while(currentTypeDeclaration != specifiedTypeDeclaration) {
				if(!currentTypeDeclaration.isBound)
					throw CompilerError(source,
						"Specified type declaration of self reference not found in its surrounding type declaration.")
				val parentProperty = constructor.buildGetPropertyPointer(currentTypeDeclaration.llvmType, currentValue,
					Context.PARENT_PROPERTY_INDEX, "_parentProperty")
				currentValue = constructor.buildLoad(constructor.pointerType, parentProperty, "_parent")
				currentTypeDeclaration = currentTypeDeclaration.parent?.scope?.getSurroundingTypeDeclaration()
					?: throw CompilerError(source,
						"Specified type declaration of self reference not found in its surrounding type declaration.")
			}
		}
		return currentValue
	}
}
