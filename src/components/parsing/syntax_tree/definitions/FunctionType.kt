package components.parsing.syntax_tree.definitions

import components.linting.Linter
import components.linting.semantic_model.definitions.FunctionSignature
import components.linting.semantic_model.types.FunctionType as SemanticFunctionTypeModel
import components.linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.TypeElement
import source_structure.Position

class FunctionType(start: Position, private val parameterList: ParameterTypeList?,
				   private val returnType: TypeElement?, end: Position): TypeElement(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticFunctionTypeModel {
		val parameters = parameterList?.concretizeTypes(linter, scope) ?: listOf()
		val signature = FunctionSignature(this, listOf(), parameters, returnType?.concretize(linter, scope))
		return SemanticFunctionTypeModel(this, signature)
	}

	override fun toString(): String {
		return "FunctionType${if(returnType == null) "" else " [ $returnType ]"} { ${parameterList ?: ""} }"
	}
}
