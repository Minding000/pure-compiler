package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.types.FunctionType
import messages.Message
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.Element
import java.util.LinkedList

class Function(source: Element, val functionType: FunctionType, val name: String): Value(source, functionType) {
	private val implementations = LinkedList<FunctionImplementation>()
	val isAbstract: Boolean
		get() {
			return implementations.any { implementation -> implementation.isAbstract }
		}
	var superFunction: Function? = null
		set(value) {
			field = value
			value?.let {
				for(implementation in implementations) {
					for(superImplementation in value.implementations) {
						if(implementation.signature.fulfillsInheritanceRequirementOf(superImplementation.signature)) {
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
		addUnits(functionType)
		addUnits(implementations)
	}

	constructor(source: Element, implementation: FunctionImplementation, name: String = "<anonymous function>"):
			this(source, FunctionType(source), name) {
		addImplementation(implementation)
	}

	fun addImplementation(implementation: FunctionImplementation) {
		addUnits(implementation)
		implementations.add(implementation)
		functionType.addSignature(implementation.signature)
		implementation.setParent(this)
	}

	fun removeImplementation(implementation: FunctionImplementation) {
		removeUnit(implementation)
		implementations.remove(implementation)
		functionType.removeSignature(implementation.signature)
	}

	override fun linkTypes(linter: Linter, scope: Scope) {
		super.linkTypes(linter, scope)
		ensureUniqueSignatures(linter)
	}

	override fun validate(linter: Linter) {
		super.validate(linter)
		for(implementation in implementations) { //TODO consider moving this logic to FunctionSignature (this requires having a superSignature property)
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
				if(!otherImplementation.signature.hasSameParameterTypesAs(implementation.signature))
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
