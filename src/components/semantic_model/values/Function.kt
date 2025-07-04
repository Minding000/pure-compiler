package components.semantic_model.values

import components.code_generation.llvm.models.values.FunctionObject
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.FunctionImplementation
import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.scopes.Scope
import components.semantic_model.types.FunctionType
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.declaration.Redeclaration
import java.util.*

open class Function(source: SyntaxTreeNode, scope: Scope, val name: String = "<anonymous function>",
					val functionType: FunctionType = FunctionType(source, scope)): Value(source, scope, functionType) {
	open val memberType = "function"
	var associatedTypeDeclaration: TypeDeclaration? = null
	val implementations = LinkedList<FunctionImplementation>()
	val isAbstract: Boolean
		get() = implementations.any { implementation -> implementation.isAbstract }

	init {
		functionType.associatedFunction = this
		addSemanticModels(functionType)
	}

	fun addImplementation(implementation: FunctionImplementation) {
		addSemanticModels(implementation)
		implementations.add(implementation)
		functionType.addSignature(implementation.signature)
		implementation.setParent(this)
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		super.analyseDataFlow(tracker)
		staticValue = this
	}

	override fun validate() {
		super.validate()
		ensureUniqueSignatures()
	}

	private fun ensureUniqueSignatures() {
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
				context.addIssue(Redeclaration(otherImplementation.source, memberType, otherImplementation.toString(),
					implementation.source))
			}
		}
	}

	override fun toUnit() = FunctionObject(this, implementations.map(FunctionImplementation::toUnit))
}
