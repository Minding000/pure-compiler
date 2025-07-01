package components.semantic_model.declarations

import components.semantic_model.context.ComparisonResult
import components.semantic_model.control_flow.OverGenerator
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.MutableScope
import components.semantic_model.types.Type
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.declaration.DeclarationMissingTypeOrValue
import logger.issues.resolution.ConversionAmbiguity
import java.util.*
import components.code_generation.llvm.models.declarations.ValueDeclaration as ValueDeclarationUnit

abstract class ValueDeclaration(override val source: SyntaxTreeNode, override val scope: MutableScope, val name: String,
								var providedType: Type? = null, value: Value? = null, val isConstant: Boolean = true,
								val isMutable: Boolean = false): SemanticModel(source, scope) {
	val effectiveType: Type? get() = providedType?.effectiveType
	private var hasDeterminedTypes = false
	open val value = value
	val usages = LinkedList<VariableValue>()
	var conversion: InitializerDefinition? = null
	//TODO set this in each sub-class
	lateinit var unit: ValueDeclarationUnit

	init {
		addSemanticModels(providedType, value)
	}

	override fun declare() {
		super.declare()
		scope.addValueDeclaration(this)
	}

	fun getLinkedType(): Type? {
		val type = providedType
		if(type != null) {
			type.determineTypes()
			return type
		}
		determineTypes()
		return this.providedType
	}

	override fun determineTypes() {
		if(hasDeterminedTypes)
			return
		hasDeterminedTypes = true
		determineType()
	}

	protected open fun determineType() {
		super.determineTypes()
		val value = value
		if(value == null) {
			if(providedType == null)
				context.addIssue(DeclarationMissingTypeOrValue(source, parent !is OverGenerator))
			return
		}
		val targetType = providedType
		if(value.isAssignableTo(targetType)) {
			value.setInferredType(targetType)
			return
		}
		val sourceType = value.providedType ?: return
		if(targetType == null) {
			providedType = sourceType
			return
		}
		val possibleConversions = targetType.getConversionsFrom(sourceType)
		if(possibleConversions.isEmpty()) {
			context.addIssue(TypeNotAssignable(source, sourceType, targetType))
			return
		}
		var mostSpecificConversion: InitializerDefinition? = null
		specificityPrecedenceLoop@ for(conversion in possibleConversions) {
			for(otherConversion in possibleConversions) {
				if(otherConversion === conversion)
					continue
				if(conversion.compareSpecificity(otherConversion) != ComparisonResult.HIGHER)
					continue@specificityPrecedenceLoop
			}
			value.setInferredType(conversion.getParameterTypeAt(0))
			mostSpecificConversion = conversion
		}
		if(mostSpecificConversion == null) {
			context.addIssue(ConversionAmbiguity(source, sourceType, targetType, possibleConversions))
			return
		}
		conversion = mostSpecificConversion
	}

	abstract override fun toUnit(): ValueDeclarationUnit

	data class Match(val declaration: ValueDeclaration, val whereClauseConditions: List<WhereClauseCondition>?, val type: Type?) {

		constructor(declaration: ValueDeclaration):
			this(declaration, (declaration as? ComputedPropertyDeclaration)?.whereClauseConditions, declaration.getLinkedType())

		fun withTypeSubstitutions(typeSubstitutions: Map<TypeDeclaration, Type>): Match {
			return Match(declaration, whereClauseConditions?.map { condition -> condition.withTypeSubstitutions(typeSubstitutions) },
				type?.withTypeSubstitutions(typeSubstitutions))
		}
	}
}
