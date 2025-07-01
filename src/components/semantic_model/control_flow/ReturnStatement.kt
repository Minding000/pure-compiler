package components.semantic_model.control_flow

import components.code_generation.llvm.models.control_flow.ReturnStatement
import components.semantic_model.context.ComparisonResult
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
	private var targetInitializer: InitializerDefinition? = null
	var targetFunction: FunctionImplementation? = null
	var targetComputedProperty: ComputedPropertyDeclaration? = null
	var conversion: InitializerDefinition? = null

	init {
		addSemanticModels(value)
	}

	override fun determineTypes() {
		super.determineTypes()
		determineTargetFunction()
	}

	private fun determineTargetFunction() {
		val surroundingInitializer = scope.getSurroundingInitializer()
		if(surroundingInitializer != null) {
			targetInitializer = surroundingInitializer
			return
		}
		val surroundingFunction = scope.getSurroundingFunction()
		if(surroundingFunction != null) {
			targetFunction = surroundingFunction
			surroundingFunction.mightReturnValue = true
			return
		}
		val surroundingComputedProperty = scope.getSurroundingComputedProperty()
		if(surroundingComputedProperty != null) {
			targetComputedProperty = surroundingComputedProperty
			return
		}
		context.addIssue(ReturnStatementOutsideOfCallable(source))
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
		if(targetInitializer != null) {
			if(value != null)
				context.addIssue(RedundantReturnValue(source))
			return
		}
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
			return
		}
		if(SpecialType.NOTHING.matches(returnType)) {
			context.addIssue(RedundantReturnValue(source))
		} else if(value.isAssignableTo(returnType)) {
			value.setInferredType(returnType)
		} else {
			val valueType = value.providedType
			if(valueType != null) {
				val possibleConversions = returnType.getConversionsFrom(valueType)
				if(possibleConversions.isEmpty()) {
					context.addIssue(ReturnValueTypeMismatch(source, valueType, returnType))
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
					context.addIssue(ConversionAmbiguity(source, valueType, returnType, possibleConversions))
					return
				}
				conversion = mostSpecificConversion
			}
		}
	}

	override fun toUnit() = ReturnStatement(this, value?.toUnit())
}
