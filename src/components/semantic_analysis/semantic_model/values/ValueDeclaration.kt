package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.definition.DeclarationMissingTypeOrValue
import logger.issues.resolution.ConversionAmbiguity
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
				linter.addIssue(DeclarationMissingTypeOrValue(source))
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
			val conversions = targetType.getConversionsFrom(linter, sourceType)
			if(conversions.isNotEmpty()) {
				if(conversions.size > 1) {
					linter.addIssue(ConversionAmbiguity(source, sourceType, targetType, conversions))
					return
				}
				conversion = conversions.first()
				return
			}
			linter.addIssue(TypeNotAssignable(source, sourceType, targetType))
		}
	}
}
