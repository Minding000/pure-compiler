package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.operations.IndexAccess
import components.semantic_analysis.semantic_model.operations.MemberAccess
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import messages.Message
import components.syntax_parser.syntax_tree.literals.SuperReference as SuperReferenceSyntaxTree

open class SuperReference(override val source: SuperReferenceSyntaxTree, scope: Scope, private val specifier: ObjectType?):
	Value(source, scope) {

	init {
		addUnits(specifier)
	}

	override fun linkValues(linter: Linter) {
		val surroundingDefinition = scope.getSurroundingDefinition()
		if(surroundingDefinition == null) {
			linter.addMessage(source, "Super references are not allowed outside of type definitions.", Message.Type.ERROR)
			return
		}
		var superTypes = surroundingDefinition.getAllSuperTypes()
		specifier?.definition?.let { specifierDefinition ->
			superTypes = superTypes.filter { superType -> matchesSpecifier(superType, specifierDefinition) }
			if(superTypes.isEmpty()) {
				linter.addMessage(source, "'${surroundingDefinition.name}' does not inherit from '$specifier'.",
					Message.Type.ERROR)
				return
			}
		}
		val possibleTargetTypes = when(val parent = parent) {
			is MemberAccess -> {
				if(parent.member is InitializerReference) {
					if(!isInInitializer()) {
						linter.addMessage(source, "The super initializer can only be called from initializers.",
							Message.Type.ERROR)
						return
					}
				}
				parent.filterForPossibleTargetTypes(superTypes)
			}
			is IndexAccess -> {
				parent.filterForPossibleTargetTypes(superTypes)
			}
			else -> {
				linter.addMessage(source, "Super references are not allowed outside of member and index accesses.",
					Message.Type.ERROR)
				return
			}
		}
		if(possibleTargetTypes.isEmpty()) {
			linter.addMessage(source, "The specified member does not exist on any super type of this type definition.",
				Message.Type.ERROR)
		} else if(possibleTargetTypes.size > 1) {
			var message = "The super reference is ambiguous. Possible targets are:"
			for(superType in possibleTargetTypes)
				message += "\n - $superType"
			linter.addMessage(source, message, Message.Type.ERROR)
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
