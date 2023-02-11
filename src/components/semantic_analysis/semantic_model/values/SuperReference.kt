package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.operations.MemberAccess
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import messages.Message
import components.syntax_parser.syntax_tree.literals.SuperReference as SuperReferenceSyntaxTree

open class SuperReference(override val source: SuperReferenceSyntaxTree, private val specifier: ObjectType?): Value(source) {
	var definition: TypeDefinition? = null

	init {
		addUnits(specifier)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		//TODO:
		// - get all surrounding definitions
		//   - lint if there are none
		// - if specifier is defined
		//   - find specified definition
		//     - lint if not found
		//   - use type
		// - use and-union of all types
		//   - make sure ambiguities result in errors (but try to avoid them)

		val surroundingDefinition = scope.getSurroundingDefinition()
		if(surroundingDefinition == null) {
			linter.addMessage(source, "Super references are not allowed outside of type definitions.", Message.Type.ERROR)
		} else {
			//TODO get super type definition
			// - if there are multiple super types (and-union type) then determine which one to use by requiring an addition identifier
			definition = surroundingDefinition
			val typeParameters = surroundingDefinition.scope.getGenericTypeDefinitions().map { ObjectType(it) }
			type = ObjectType(typeParameters, surroundingDefinition)
		}
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		val parent = parent
		if(parent is MemberAccess) {
			if(parent.member is InitializerReference) {
				if(!isInInitializer(this))
					linter.addMessage(source, "The super initializer can only be called from initializers.", Message.Type.ERROR)
			}
		} else {
			linter.addMessage(source, "Super references are not allowed outside of member accesses.", Message.Type.ERROR)
		}
	}

	private fun isInInitializer(unit: Unit): Boolean {
		if(unit is InitializerDefinition)
			return true
		if(unit is FunctionImplementation)
			return false
		return isInInitializer(unit.parent ?: return false)
	}
}
