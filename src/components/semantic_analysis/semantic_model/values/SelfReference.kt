package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import messages.Message
import components.syntax_parser.syntax_tree.literals.SelfReference as SelfReferenceSyntaxTree

open class SelfReference(override val source: SelfReferenceSyntaxTree): Value(source) {
	var definition: TypeDefinition? = null

	override fun linkValues(linter: Linter, scope: Scope) {
		val surroundingDefinition = scope.getSurroundingDefinition()
		if(surroundingDefinition == null) {
			linter.addMessage(source, "Self references are not allowed outside of type definitions.", Message.Type.ERROR)
		} else {
			definition = surroundingDefinition
			val typeParameters = surroundingDefinition.scope.getGenericTypeDefinitions().map { ObjectType(it) }
			type = ObjectType(typeParameters, surroundingDefinition)
		}
	}
}
