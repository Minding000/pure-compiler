package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.ObjectType
import messages.Message
import components.syntax_parser.syntax_tree.literals.SelfReference as SelfReferenceSyntaxTree

open class SelfReference(override val source: SelfReferenceSyntaxTree, private val specifier: ObjectType?): Value(source) {
	var definition: TypeDefinition? = null

	init {
		addUnits(specifier)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		if(specifier == null) {
			definition = scope.getSurroundingDefinition()
			if(definition == null)
				linter.addMessage(source, "Self references are not allowed outside of type definitions.", Message.Type.ERROR)
		} else {
			specifier.definition?.let { specifierDefinition ->
				definition = specifierDefinition
				if(!isSurroundedBy(scope, specifierDefinition)) //TODO check if surrounding class is bound (write tests)
					linter.addMessage(source, "Self references can only specify surrounding types.", Message.Type.ERROR)
			}
		}
		definition?.let { definition ->
			val typeParameters = definition.scope.getGenericTypeDefinitions().map { ObjectType(it) }
			type = ObjectType(typeParameters, definition)
		}
	}

	private fun isSurroundedBy(scope: Scope, definition: TypeDefinition): Boolean {
		var currentScope = scope
		while(true) {
			currentScope = if(currentScope is TypeScope) {
				if(currentScope.typeDefinition == definition)
					return true
				currentScope.parentScope
			} else if(currentScope is BlockScope) {
				currentScope.parentScope
			} else break
		}
		return false
	}
}
