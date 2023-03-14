package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.FunctionSignature
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.FunctionType
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.definition.Redeclaration
import java.util.*

open class Function(source: Element, scope: Scope, val name: String = "<anonymous function>",
					val functionType: FunctionType = FunctionType(source, scope)): Value(source, scope, functionType) {
	open val memberType = "function"
	protected val implementations = LinkedList<FunctionImplementation>()
	val isAbstract: Boolean
		get() {
			return implementations.any { implementation -> implementation.isAbstract }
		}

	init {
		addUnits(functionType)
		addUnits(implementations)
	}

	fun addImplementation(implementation: FunctionImplementation) {
		addUnits(implementation)
		implementations.add(implementation)
		functionType.addSignature(implementation.signature)
		implementation.setParent(this)
	}

	fun getImplementationBySignature(signature: FunctionSignature): FunctionImplementation? {
		for(implementation in implementations) {
			if(implementation.signature == signature)
				return implementation
		}
		return null
	}

	fun removeImplementation(implementation: FunctionImplementation) {
		removeUnit(implementation)
		implementations.remove(implementation)
		functionType.removeSignature(implementation.signature)
	}

	override fun linkTypes(linter: Linter) {
		super.linkTypes(linter)
		ensureUniqueSignatures(linter)
	}

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		staticValue = this
	}

	private fun ensureUniqueSignatures(linter: Linter) {
		val redeclarations = LinkedList<FunctionImplementation>()
		for(initializerIndex in 0 until implementations.size - 1) {
			val implementation = implementations[initializerIndex]
			if(redeclarations.contains(implementation))
				continue
			for(otherImplementationIndex in initializerIndex + 1 until implementations.size) {
				val otherImplementation = implementations[otherImplementationIndex]
				if(!otherImplementation.signature.hasSameParameterTypesAs(implementation.signature))
					continue
				redeclarations.add(otherImplementation)
				linter.addIssue(Redeclaration(otherImplementation.source, memberType, otherImplementation.toString(),
					implementation.source))
			}
		}
		for(implementation in redeclarations)
			removeImplementation(implementation)
	}
}
