package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.FunctionSignature
import linting.semantic_model.literals.FunctionType
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.TypeElement
import source_structure.Position

class FunctionType(start: Position, private val parameterList: ParameterTypeList?,
				   private val returnType: TypeElement?, end: Position): TypeElement(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): FunctionType {
		val parameters = parameterList?.concretizeTypes(linter, scope) ?: listOf()
		val signature = FunctionSignature(this, listOf(), parameters, returnType?.concretize(linter, scope))
		return FunctionType(this, signature)
	}

	override fun toString(): String {
		return "FunctionType${if(returnType == null) "" else " [ $returnType ]"} { ${parameterList ?: ""} }"
	}
}