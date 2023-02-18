package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import messages.Message
import java.util.*

abstract class ValueDeclaration(override val source: Element, val name: String, var type: Type? = null, value: Value? = null,
								val isConstant: Boolean = true, val isMutable: Boolean = false): Unit(source) {
	open val value = value
	val usages = LinkedList<VariableValue>()

	init {
		addUnits(type, value)
	}

	abstract fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): ValueDeclaration

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		val value = value
		if(value == null) {
			if(type == null)
				linter.addMessage(source, "Type or value is required.", Message.Type.ERROR)
		} else {
			if(value.isAssignableTo(type)) {
				value.setInferredType(type)
			} else if(type == null) {
				type = value.type
			} else {
				linter.addMessage(source, "Type '${value.type}' is not assignable to type '$type'.",
					Message.Type.ERROR)
			}
		}
	}
}
