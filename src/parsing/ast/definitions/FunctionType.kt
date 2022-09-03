package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.FunctionSignature
import linter.elements.literals.FunctionType
import linter.scopes.MutableScope
import parsing.ast.general.TypeElement
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