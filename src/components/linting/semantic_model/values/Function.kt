package components.linting.semantic_model.values

import components.linting.Linter
import components.linting.semantic_model.definitions.FunctionImplementation
import components.linting.semantic_model.types.FunctionType
import messages.Message
import components.linting.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element
import java.util.LinkedList

class Function(source: Element, private val implementations: MutableList<FunctionImplementation>,
			   val functionType: FunctionType, val name: String): Value(source, functionType) {
	var superFunction: Function? = null
		set(value) {
			field = value
			value?.let {
				for(implementation in implementations) {
					for(superImplementation in value.implementations) {
						if(superImplementation.signature == implementation.signature) {
							implementation.superFunctionImplementation = superImplementation
							break
						}
					}
				}
			}
			functionType.superFunctionType = value?.functionType
		}

	init {
		staticValue = this
		units.add(functionType)
		units.addAll(implementations)
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
		val redeclarations = LinkedList<FunctionImplementation>()
		for(initializerIndex in 0 until implementations.size - 1) {
			val implementation = implementations[initializerIndex]
			if(redeclarations.contains(implementation))
				continue
			for(otherImplementationIndex in initializerIndex + 1 until  implementations.size) {
				val otherImplementation = implementations[otherImplementationIndex]
				if(otherImplementation.signature != implementation.signature)
					continue
				redeclarations.add(otherImplementation)
				linter.addMessage(otherImplementation.source, "Redeclaration of function " +
						"'$name${otherImplementation.signature.toString(false)}', " +
						"previously declared in ${implementation.source.getStartString()}.",
					Message.Type.ERROR) //TODO include parent type name if applicable
			}
		}
		for(implementation in redeclarations)
			removeImplementation(implementation)
	}
}
