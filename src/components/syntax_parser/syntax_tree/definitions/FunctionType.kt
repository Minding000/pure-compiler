package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.definitions.FunctionSignature
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeElement
import source_structure.Position
import components.semantic_analysis.semantic_model.types.FunctionType as SemanticFunctionTypeModel

class FunctionType(start: Position, private val parameterList: ParameterTypeList?, private val returnType: TypeElement?, end: Position):
	TypeElement(start, end) {

	override fun toSemanticModel(scope: MutableScope): SemanticFunctionTypeModel {
		val functionScope = BlockScope(scope)
		val parameters = parameterList?.toSemanticModels(functionScope) ?: listOf()
		val signature = FunctionSignature(this, functionScope, listOf(), parameters, returnType?.toSemanticModel(functionScope))
		return SemanticFunctionTypeModel(this, scope, signature)
	}

	override fun toString(): String {
		return "FunctionType${if(returnType == null) "" else " [ $returnType ]"} { ${parameterList ?: ""} }"
	}
}
