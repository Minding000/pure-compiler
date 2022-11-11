package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.syntax_parser.syntax_tree.literals.SelfReference as SelfReferenceSyntaxTree

open class SelfReference(override val source: SelfReferenceSyntaxTree): Value(source) {
	var definition: TypeDefinition? = null

	override fun linkValues(linter: Linter, scope: Scope) {
		definition = scope.getSurroundingDefinition()
		definition?.let { definition ->
			type = ObjectType(definition)
		}
	}
}
