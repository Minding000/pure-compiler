package components.semantic_model.control_flow

import components.code_generation.llvm.LlvmConstructor
import components.semantic_model.context.Context
import components.semantic_model.context.VariableTracker
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.declarations.FunctionImplementation
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import errors.internal.CompilerError
import components.syntax_parser.syntax_tree.control_flow.RaiseStatement as RaiseStatementSyntaxTree

class RaiseStatement(override val source: RaiseStatementSyntaxTree, scope: Scope, val value: Value): SemanticModel(source, scope) {
	override val isInterruptingExecutionBasedOnStructure = true
	override val isInterruptingExecutionBasedOnStaticEvaluation = true
	private var targetComputedProperty: ComputedPropertyDeclaration? = null
	private var targetFunction: FunctionImplementation? = null

	init {
		addSemanticModels(value)
	}

	override fun determineTypes() {
		super.determineTypes()
		val surroundingComputedProperty = scope.getSurroundingComputedProperty()
		if(surroundingComputedProperty != null) {
			targetComputedProperty = surroundingComputedProperty
			return
		}
		val surroundingFunction = scope.getSurroundingFunction()
		if(surroundingFunction == null) {
			//TODO this should be an issue instead or raise statements should be allowed in any context
			throw CompilerError(source, "Raise statement outside of function.")
		}
		targetFunction = surroundingFunction
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		value.analyseDataFlow(tracker)
		tracker.registerRaiseStatement()
	}

	override fun compile(constructor: LlvmConstructor) {
		super.compile(constructor)
		val exceptionAddress = constructor.getParameter(constructor.getParentFunction(), Context.EXCEPTION_PARAMETER_INDEX)
		constructor.buildStore(value.getLlvmValue(constructor), exceptionAddress)
		context.handleException(constructor, parent)
	}
}
