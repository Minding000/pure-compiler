package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.definition.DeclarationMissingTypeOrValue
import logger.issues.resolution.ConversionAmbiguity
import java.util.*

abstract class ValueDeclaration(override val source: Element, override val scope: MutableScope, val name: String, var type: Type? = null,
								value: Value? = null, val isConstant: Boolean = true, val isMutable: Boolean = false,
								val isSpecificCopy: Boolean = false): Unit(source, scope) {
	private var hasDeterminedTypes = isSpecificCopy
	open val value = value
	val usages = LinkedList<VariableValue>()
	var conversion: InitializerDefinition? = null

	init {
		addUnits(type, value)
	}

	abstract fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): ValueDeclaration

	override fun declare(linter: Linter) {
		super.declare(linter)
		scope.declareValue(linter, this)
	}

	open fun getType(linter: Linter): Type? {
		val type = type
		if(type != null) {
			type.determineTypes(linter)
			return type
		}
		determineTypes(linter)
		return this.type
	}

	override fun determineTypes(linter: Linter) {
		if(hasDeterminedTypes)
			return
		hasDeterminedTypes = true
		determineType(linter)
	}

	protected open fun determineType(linter: Linter) {
		super.determineTypes(linter)
		val value = value
		if(value == null) {
			if(type == null)
				linter.addIssue(DeclarationMissingTypeOrValue(source))
			return
		}
		val targetType = type
		if(value.isAssignableTo(targetType)) {
			value.setInferredType(targetType)
			return
		}
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
