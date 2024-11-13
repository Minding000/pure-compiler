package components.semantic_model.control_flow

import components.code_generation.llvm.models.control_flow.RaiseStatement
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.declarations.FunctionImplementation
import components.semantic_model.declarations.InitializerDefinition
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import errors.internal.CompilerError
import components.syntax_parser.syntax_tree.control_flow.RaiseStatement as RaiseStatementSyntaxTree

class RaiseStatement(override val source: RaiseStatementSyntaxTree, scope: Scope, val value: Value): SemanticModel(source, scope) {
	override val isInterruptingExecutionBasedOnStructure = true
	override val isInterruptingExecutionBasedOnStaticEvaluation = true
	var targetInitializer: InitializerDefinition? = null
	var targetFunction: FunctionImplementation? = null
	var targetComputedProperty: ComputedPropertyDeclaration? = null

	init {
		addSemanticModels(value)
	}

	override fun determineTypes() {
		super.determineTypes()
		val surroundingInitializer = scope.getSurroundingInitializer()
		if(surroundingInitializer != null) {
			targetInitializer = surroundingInitializer
			return
		}
		val surroundingFunction = scope.getSurroundingFunction()
		if(surroundingFunction != null) {
			targetFunction = surroundingFunction
			return
		}
		val surroundingComputedProperty = scope.getSurroundingComputedProperty()
		if(surroundingComputedProperty != null) {
			targetComputedProperty = surroundingComputedProperty
			return
		}
		//TODO this should be an issue instead or raise statements should be allowed in any context
		throw CompilerError(source, "Raise statement outside of callable.")
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		value.analyseDataFlow(tracker)
		tracker.registerRaiseStatement()
	}

	override fun toUnit() = RaiseStatement(this, value.toUnit())
}
