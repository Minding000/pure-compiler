package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.semantic_model.control_flow.LoopStatement
import components.semantic_analysis.semantic_model.declarations.FunctionImplementation
import components.semantic_analysis.semantic_model.declarations.FunctionSignature
import components.semantic_analysis.semantic_model.declarations.TypeDeclaration
import components.semantic_analysis.semantic_model.types.FunctionType
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.Operator
import components.semantic_analysis.semantic_model.values.Value
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.semantic_analysis.semantic_model.values.VariableValue

abstract class Scope {

	//TODO return Pair<TypeDeclaration, StaticType> instead
	abstract fun getTypeDeclaration(name: String): TypeDeclaration?

	abstract fun getValueDeclaration(name: String): Pair<ValueDeclaration?, Type?>
	open fun getValueDeclaration(variable: VariableValue) = getValueDeclaration(variable.name)

	fun getOperator(kind: Operator.Kind) = getOperator(kind, emptyList())

	fun getOperator(kind: Operator.Kind, suppliedType: Value) = getOperator(kind, listOf(suppliedType))

	open fun getOperator(kind: Operator.Kind, suppliedValues: List<Value>): FunctionSignature? {
		val (valueDeclaration) = getValueDeclaration(kind.stringRepresentation)
		val operator = valueDeclaration?.getLinkedType() as? FunctionType
		return operator?.getSignature(suppliedValues)
	}

	fun getIndexOperator(suppliedTypes: List<Type>, suppliedIndexValues: List<Value>, suppliedParameterValue: Value?)
		= getIndexOperator(suppliedTypes, suppliedIndexValues, listOfNotNull(suppliedParameterValue))

	open fun getIndexOperator(suppliedTypes: List<Type>, suppliedIndexValues: List<Value>,
							  suppliedParameterValues: List<Value>): FunctionSignature? {
		val kind = if(suppliedParameterValues.isEmpty()) Operator.Kind.BRACKETS_GET else Operator.Kind.BRACKETS_SET
		return getOperator(kind, listOf(*suppliedIndexValues.toTypedArray(), *suppliedParameterValues.toTypedArray()))
	}

	open fun getSurroundingTypeDeclaration(): TypeDeclaration? = null

	open fun getSurroundingFunction(): FunctionImplementation? = null

	open fun getSurroundingLoop(): LoopStatement? = null
}
