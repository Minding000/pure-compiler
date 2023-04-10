package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import logger.issues.resolution.SelfReferenceOutsideOfTypeDefinition
import logger.issues.resolution.SelfReferenceSpecifierNotBound
import components.syntax_parser.syntax_tree.literals.SelfReference as SelfReferenceSyntaxTree

open class SelfReference(override val source: SelfReferenceSyntaxTree, scope: Scope, private val specifier: ObjectType?):
	Value(source, scope) {
	var definition: TypeDefinition? = null

	init {
		addUnits(specifier)
		specifier?.setIsNonSpecificContext()
	}

	override fun linkValues(linter: Linter) {
		val surroundingDefinition = scope.getSurroundingDefinition()
		if(surroundingDefinition == null) {
			linter.addIssue(SelfReferenceOutsideOfTypeDefinition(source))
			return
		}
		if(specifier == null) {
			definition = surroundingDefinition
		} else {
			val specifierDefinition = specifier.definition
			if(specifierDefinition != null) {
				if(isBoundTo(surroundingDefinition, specifierDefinition))
					definition = specifierDefinition
				else
					linter.addIssue(SelfReferenceSpecifierNotBound(source, surroundingDefinition, specifierDefinition))
			}
		}
		definition?.let { definition ->
			val typeParameters = definition.scope.getGenericTypeDefinitions().map { ObjectType(it) }
			type = ObjectType(typeParameters, definition)
			type?.resolveGenerics(linter) //TODO find cleaner way to create ObjectType and call uncalled event stage functions
			addUnits(type)
		}
	}

	private fun isBoundTo(childDefinition: TypeDefinition, parentDefinition: TypeDefinition): Boolean {
		var currentDefinition = childDefinition
		while(true) {
			if(currentDefinition == parentDefinition)
				return true
			if(!currentDefinition.isBound)
				break
			currentDefinition = currentDefinition.parentTypeDefinition ?: break
		}
		return false
	}
}
