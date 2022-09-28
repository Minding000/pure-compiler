package linting.semantic_model.values

import linting.Linter
import linting.semantic_model.definitions.FunctionImplementation
import linting.semantic_model.literals.FunctionType
import messages.Message
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.general.Element
import java.util.LinkedList

class Function(source: Element, private val implementations: MutableList<FunctionImplementation>,
			   val functionType: FunctionType, val name: String): Value(source, functionType) {
	var superFunction: Function? = null

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

	fun removeImplementation(implementation: FunctionImplementation) {
		units.remove(implementation)
		implementations.remove(implementation)
		functionType.removeSignature(implementation.signature)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, scope)
		ensureUniqueSignatures(linter)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		for(implementation in implementations) {
			if(functionType.superFunctionType?.hasSignature(implementation.signature) == true) {
				if(!implementation.isOverriding)
					linter.addMessage(implementation.source, "Missing 'overriding' keyword", Message.Type.WARNING)
			} else {
				if(implementation.isOverriding)
					linter.addMessage(implementation.source, "'overriding' keyword is used, but the function doesn't have a super function", Message.Type.WARNING)
			}
		}
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
					linter.addMessage(otherImplementation.source, "Redeclaration of function " +
									"'$name${otherImplementation.signature.toString(false)}', " +
									"previously declared in ${implementation.source.getStartString()}.",
						Message.Type.ERROR)
				}
			}
		}
		for(implementation in redeclarations)
			removeImplementation(implementation)
	}
}