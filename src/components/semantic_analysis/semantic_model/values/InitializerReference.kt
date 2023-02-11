package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.StaticType
import messages.Message
import components.syntax_parser.syntax_tree.literals.InitializerReference as InitializerReferenceSyntaxTree

open class InitializerReference(override val source: InitializerReferenceSyntaxTree): Value(source) {
	var definition: TypeDefinition? = null

	override fun linkValues(linter: Linter, scope: Scope) {
		//TODO get surrounding initializer instead, then get type definition from it?
		val surroundingDefinition = scope.getSurroundingDefinition()
		if(surroundingDefinition == null) {
			linter.addMessage(source, "Initializer references are not allowed outside of initializers.", Message.Type.ERROR)
		} else {
			definition = surroundingDefinition
			type = StaticType(surroundingDefinition)
		}
	}
}
