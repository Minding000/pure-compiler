package components.semantic_model.control_flow

import components.code_generation.llvm.LlvmConstructor
import components.code_generation.llvm.ValueConverter
import components.semantic_model.context.SpecialType
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.declarations.FunctionImplementation
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.resolution.ConversionAmbiguity
import logger.issues.returns.RedundantReturnValue
import logger.issues.returns.ReturnStatementMissingValue
import logger.issues.returns.ReturnStatementOutsideOfCallable
import logger.issues.returns.ReturnValueTypeMismatch

class ReturnStatement(override val source: SyntaxTreeNode, scope: Scope, val value: Value?): SemanticModel(source, scope) {
	override val isInterruptingExecutionBasedOnStructure = true
	override val isInterruptingExecutionBasedOnStaticEvaluation = true
	private var targetFunction: FunctionImplementation? = null
	private var targetComputedProperty: ComputedPropertyDeclaration? = null
	private var conversion: InitializerDefinition? = null

	init {
		addSemanticModels(value)
	}

	override fun determineTypes() {
		super.determineTypes()
		determineTargetFunction()
	}

	private fun determineTargetFunction() {
		val surroundingComputedProperty = scope.getSurroundingComputedProperty()
		if(surroundingComputedProperty != null) {
			targetComputedProperty = surroundingComputedProperty
			return
		}
		val surroundingFunction = scope.getSurroundingFunction()
		if(surroundingFunction == null) {
			context.addIssue(ReturnStatementOutsideOfCallable(source))
			return
		}
		targetFunction = surroundingFunction
		surroundingFunction.mightReturnValue = true
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		value?.analyseDataFlow(tracker)
		tracker.registerReturnStatement()
	}

	override fun validate() {
		super.validate()
		validateReturnType()
	}

	private fun validateReturnType() {
		var returnType = targetFunction?.signature?.returnType
		val targetComputedProperty = targetComputedProperty
		if(targetComputedProperty != null) {
			val getter = targetComputedProperty.getterErrorHandlingContext
			returnType = if(getter != null && isIn(getter))
				targetComputedProperty.getterReturnType
			else
				targetComputedProperty.setterReturnType
		}
		if(returnType == null)
			return
		if(value == null) {
			if(!SpecialType.NOTHING.matches(returnType))
				context.addIssue(ReturnStatementMissingValue(source))
		} else {
			if(SpecialType.NOTHING.matches(returnType)) {
				context.addIssue(RedundantReturnValue(source))
			} else if(value.isAssignableTo(returnType)) {
				value.setInferredType(returnType)
			} else {
				val valueType = value.providedType
				if(valueType != null) {
					val conversions = returnType.getConversionsFrom(valueType)
					if(conversions.isNotEmpty()) {
						if(conversions.size > 1) {
							context.addIssue(ConversionAmbiguity(source, valueType, returnType, conversions))
							return
						}
						conversion = conversions.first()
						return
					}
					context.addIssue(ReturnValueTypeMismatch(source, valueType, returnType))
				}
			}
		}
	}

	override fun compile(constructor: LlvmConstructor) {
		val errorHandlingContext = scope.getSurroundingAlwaysBlock()
		if(value == null) {
			errorHandlingContext?.runAlwaysBlock(constructor)
			constructor.buildReturn()
			return
		}
		val declaredReturnType = targetFunction?.signature?.returnType ?: targetComputedProperty?.getterReturnType
		val returnValue = ValueConverter.convertIfRequired(this, constructor, value.getLlvmValue(constructor), value.effectiveType,
			value.hasGenericType, declaredReturnType, false, conversion)
		errorHandlingContext?.runAlwaysBlock(constructor)
		constructor.buildReturn(returnValue)
	}
}
