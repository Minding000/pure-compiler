package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.operations.IndexAccess
import components.semantic_analysis.semantic_model.operations.MemberAccess
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import messages.Message
import components.syntax_parser.syntax_tree.literals.SuperReference as SuperReferenceSyntaxTree

open class SuperReference(override val source: SuperReferenceSyntaxTree, private val specifier: ObjectType?): Value(source) {
	var definition: TypeDefinition? = null

	init {
		addUnits(specifier)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
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
		when(val parent = parent) {
			is MemberAccess -> {
				if(parent.member is InitializerReference) {
					if(!isInInitializer(this)) {
						linter.addMessage(source, "The super initializer can only be called from initializers.",
							Message.Type.ERROR)
						return
					}
				}
				superTypes = parent.filterForPossibleTargetTypes(superTypes)
			}
			is IndexAccess -> {
				superTypes = parent.filterForPossibleTargetTypes(superTypes)
			}
			else -> {
				linter.addMessage(source, "Super references are not allowed outside of member and index accesses.",
					Message.Type.ERROR)
				return
			}
		}
		if(superTypes.isEmpty()) {
			linter.addMessage(source, "The specified member does not exist on any super type of this type definition.",
				Message.Type.ERROR)
		} else if(superTypes.size > 1) {
			var message = "The super reference is ambiguous. Possible targets are:"
			for(superType in superTypes)
				message += "\n - $superType"
			linter.addMessage(source, message, Message.Type.ERROR)
		} else {
			val intendedType = superTypes.first()
			definition = intendedType.definition
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

	private fun isInInitializer(unit: Unit): Boolean {
		if(unit is InitializerDefinition)
			return true
		if(unit is FunctionImplementation)
			return false
		return isInInitializer(unit.parent ?: return false)
	}
}
