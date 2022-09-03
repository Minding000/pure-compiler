package linter.elements.values

import linter.elements.definitions.FunctionImplementation
import linter.elements.literals.FunctionType
import parsing.ast.general.Element

class Function(source: Element, val implementations: MutableList<FunctionImplementation>, type: FunctionType):
	Value(source, type) {

	init {
		units.addAll(implementations)
	}

	constructor(source: Element, implementation: FunctionImplementation):
			this(source, mutableListOf(implementation), FunctionType(source, implementation.signature))

	fun addImplementation(implementation: FunctionImplementation) {
		implementations.add(implementation)
	}
}