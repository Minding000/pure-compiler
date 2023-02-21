package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.InterfaceScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.StaticType
import messages.Message
import components.syntax_parser.syntax_tree.literals.InitializerReference as InitializerReferenceSyntaxTree

open class InitializerReference(override val source: InitializerReferenceSyntaxTree): Value(source) {

	override fun linkValues(linter: Linter, scope: Scope) {
		if(scope is InterfaceScope && scope.type is StaticType) {
			type = scope.type
		} else {
			val surroundingDefinition = scope.getSurroundingDefinition()
			if(surroundingDefinition == null || !isInInitializer()) {
				linter.addMessage(source, "Initializer references are not allowed outside of initializers.", Message.Type.ERROR)
			} else {
				type = StaticType(surroundingDefinition)
			}
		}
	}
}
