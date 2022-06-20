package linter.scopes

import linter.elements.definitions.FunctionDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.values.TypeDefinition
import linter.elements.values.Value
import linter.elements.values.VariableValueDeclaration
import java.util.*

class InterfaceScope: Scope() {
	val scopes = LinkedList<MutableScope>()

	override fun resolveType(name: String): TypeDefinition? {
		for(scope in scopes) {
			val definition = scope.resolveType(name)
			if(definition != null)
				return definition
		}
		return null
	}

	override fun resolveReference(name: String): VariableValueDeclaration? {
		for(scope in scopes) {
			val declaration = scope.resolveReference(name)
			if(declaration != null)
				return declaration
		}
		return null
	}

	override fun resolveFunction(name: String, suppliedValues: List<Value>): FunctionDefinition? {
		for(scope in scopes) {
			val declaration = scope.resolveFunction(name, suppliedValues)
			if(declaration != null)
				return declaration
		}
		return null
	}

	override fun resolveOperator(name: String, suppliedValues: List<Value>): OperatorDefinition? {
		for(scope in scopes) {
			val declaration = scope.resolveOperator(name, suppliedValues)
			if(declaration != null)
				return declaration
		}
		return null
	}
}