package components.semantic_model.scopes

import components.semantic_model.control_flow.LoopStatement
import components.semantic_model.declarations.*
import components.semantic_model.general.ErrorHandlingContext
import components.semantic_model.general.SemanticModel
import components.semantic_model.types.FunctionType
import components.semantic_model.types.Type
import components.semantic_model.values.Operator
import components.semantic_model.values.Value
import components.semantic_model.values.VariableValue

abstract class Scope {

	//TODO return Pair<TypeDeclaration, StaticType> instead
	abstract fun getTypeDeclaration(name: String): TypeDeclaration?

	abstract fun getValueDeclaration(name: String): ValueDeclaration.Match?
	open fun getValueDeclaration(variable: VariableValue) = getValueDeclaration(variable.name)

	fun getOperator(kind: Operator.Kind, suppliedValue: Value) = getOperator(kind, listOf(suppliedValue))

	open fun getOperator(kind: Operator.Kind, suppliedValues: List<Value> = emptyList()): FunctionType.Match? {
		val operator = getValueDeclaration(kind.stringRepresentation)?.type as? FunctionType
		return operator?.getSignature(suppliedValues)
	}

	fun getIndexOperator(suppliedTypes: List<Type>, suppliedIndexValues: List<Value>, suppliedParameterValue: Value?) =
		getIndexOperator(suppliedTypes, suppliedIndexValues, listOfNotNull(suppliedParameterValue))

	open fun getIndexOperator(suppliedTypes: List<Type>, suppliedIndexValues: List<Value>,
							  suppliedParameterValues: List<Value>): FunctionType.Match? {
		val kind = if(suppliedParameterValues.isEmpty()) Operator.Kind.BRACKETS_GET else Operator.Kind.BRACKETS_SET
		return getOperator(kind, listOf(*suppliedIndexValues.toTypedArray(), *suppliedParameterValues.toTypedArray()))
	}

	/** Similar to SemanticModel::getSurrounding<TypeDeclaration>, but takes explicit parent type declarations into account. */
	open fun getSurroundingTypeDeclaration(): TypeDeclaration? = null

	/** Similar to SemanticModel::getSurrounding<InitializerDefinition>, but doesn't search outside the surrounding type declaration. */
	open fun getSurroundingInitializer(): InitializerDefinition? = null

	/** Similar to SemanticModel::getSurrounding<FunctionImplementation>, but doesn't search outside the surrounding type declaration. */
	open fun getSurroundingFunction(): FunctionImplementation? = null

	/** Similar to SemanticModel::getSurrounding<ComputedPropertyDeclaration>, but doesn't search outside the surrounding type declaration. */
	open fun getSurroundingComputedProperty(): ComputedPropertyDeclaration? = null

	/** Similar to SemanticModel::getSurrounding<LoopStatement>, but doesn't search outside the surrounding callable. */
	open fun getSurroundingLoop(): LoopStatement? = null

	/** Searches inside of the surrounding callable. Returns an error handling context and the direct child from which the search originated within. */
	open fun getSurroundingErrorHandlingContext(): Pair<ErrorHandlingContext, SemanticModel>? = null

	/** Searches inside of the surrounding callable. Returns an error handling context containing an always-block. */
	open fun getSurroundingAlwaysBlock(): ErrorHandlingContext? = null
}
