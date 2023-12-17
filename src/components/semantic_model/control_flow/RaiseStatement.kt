package components.semantic_model.control_flow

import components.code_generation.llvm.LlvmConstructor
import components.semantic_model.context.Context
import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.ComputedPropertyDeclaration
import components.semantic_model.declarations.FunctionImplementation
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.types.Type
import components.semantic_model.values.Value
import errors.internal.CompilerError
import components.syntax_parser.syntax_tree.control_flow.RaiseStatement as RaiseStatementSyntaxTree

class RaiseStatement(override val source: RaiseStatementSyntaxTree, scope: Scope, val value: Value): SemanticModel(source, scope) {
	override val isInterruptingExecution = true
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

	override fun compile(constructor: LlvmConstructor) {
		super.compile(constructor)
		val exceptionAddressLocation = constructor.getParameter(constructor.getParentFunction(), Context.EXCEPTION_PARAMETER_INDEX)
		constructor.buildStore(value.getLlvmValue(constructor), exceptionAddressLocation)
		val returnType = getReturnType()
		if(SpecialType.NOTHING.matches(returnType)) {
			constructor.buildReturn()
			return
		}
		val nullValue = if(SpecialType.BOOLEAN.matches(returnType))
			constructor.buildBoolean(false)
		else if(SpecialType.BYTE.matches(returnType))
			constructor.buildByte(0)
		else if(SpecialType.INTEGER.matches(returnType))
			constructor.buildInt32(0)
		else if(SpecialType.FLOAT.matches(returnType))
			constructor.buildFloat(0.0)
		else
			constructor.nullPointer
		constructor.buildReturn(nullValue)
	}

	private fun getReturnType(): Type? {
		var returnType = targetFunction?.signature?.returnType
		val targetComputedProperty = targetComputedProperty
		if(targetComputedProperty != null) {
			val getter = targetComputedProperty.getterErrorHandlingContext
			returnType = if(getter != null && isIn(getter))
				targetComputedProperty.getterReturnType
			else
				targetComputedProperty.setterReturnType
		}
		return returnType
	}
}
