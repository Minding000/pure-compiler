package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import messages.Message
import java.util.*

abstract class ValueDeclaration(override val source: Element, scope: Scope, val name: String, var type: Type? = null, value: Value? = null,
								val isConstant: Boolean = true, val isMutable: Boolean = false): Unit(source, scope) {
	open val value = value
	val usages = LinkedList<VariableValue>()
	var conversion: InitializerDefinition? = null

	init {
		addUnits(type, value)
	}

	abstract fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): ValueDeclaration

	override fun linkValues(linter: Linter) {
		super.linkValues(linter)
		val value = value
		if(value == null) {
			if(type == null)
				linter.addMessage(source, "Declaration requires a type or value to infer a type from.", Message.Type.ERROR)
		} else {
			val targetType = type
			if(value.isAssignableTo(targetType)) {
				value.setInferredType(targetType)
				return
			}
			value.linkValues(linter)
			val sourceType = value.type ?: return
			if(targetType == null) {
				type = sourceType
				return
			}
			val conversions = targetType.getConversionsFrom(sourceType)
			if(conversions.isNotEmpty()) {
				if(conversions.size > 1) {
					var message = "Conversion from '$sourceType' to '$targetType' needs to be explicit," +
						" because there are multiple possible conversions:"
					for(conversion in conversions)
						message += "\n - ${conversion.parentDefinition.name}"
					linter.addMessage(source, message, Message.Type.ERROR)
				} else {
					conversion = conversions.first()
				}
				return
			}
			linter.addMessage(source, "Type '${sourceType}' is not assignable to type '$targetType'.", Message.Type.ERROR)
		}
	}
}
