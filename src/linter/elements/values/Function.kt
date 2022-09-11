package linter.elements.values

import linter.Linter
import linter.elements.definitions.FunctionImplementation
import linter.elements.literals.FunctionType
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.general.Element
import java.util.LinkedList

class Function(source: Element, private val implementations: MutableList<FunctionImplementation>,
			   val functionType: FunctionType, val name: String): Value(source, functionType) {

	init {
		units.addAll(implementations)
		units.add(functionType)
	}

	constructor(source: Element, implementation: FunctionImplementation, name: String = "<anonymous function>"):
			this(source, mutableListOf(implementation), FunctionType(source, implementation.signature), name)

	fun addImplementation(implementation: FunctionImplementation) {
		units.add(implementation)
		implementations.add(implementation)
		functionType.addSignature(implementation.signature)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, scope)
		ensureUniqueSignatures(linter)
	}

	private fun ensureUniqueSignatures(linter: Linter) {
		val implementationIterator = implementations.iterator()
		val redeclarations = LinkedList<FunctionImplementation>()
		for(implementation in implementationIterator) {
			if(redeclarations.contains(implementation))
				continue
			implementationIterator.forEachRemaining { otherImplementation ->
				if(otherImplementation.signature == implementation.signature) {
					redeclarations.add(otherImplementation)
					linter.messages.add(
						Message(
							"${otherImplementation.source.getStartString()}: Redeclaration of" +
									" function '$name${otherImplementation.signature.toString(false)}'," +
									" previously declared in ${implementation.source.getStartString()}.", Message.Type.ERROR)
					)
				}
			}
		}
		implementations.removeAll(redeclarations)
	}
}