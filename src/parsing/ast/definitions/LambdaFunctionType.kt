package parsing.ast.definitions

import linter.Linter
import linter.elements.literals.LambdaFunctionType
import linter.scopes.MutableScope
import parsing.ast.general.TypeElement
import source_structure.Position
import java.util.*

class LambdaFunctionType(start: Position, private val parameterList: LambdaParameterList?,
						 private val returnType: TypeElement?, end: Position): TypeElement(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): LambdaFunctionType {
		val parameters = LinkedList<linter.elements.literals.Type>()
		if(parameterList != null) {
			for(parameter in parameterList.parameters)
				parameters.add(parameter.concretize(linter, scope))
		}
		return LambdaFunctionType(this, parameters, returnType?.concretize(linter, scope))
	}

	override fun toString(): String {
		return "LambdaFunctionType${if(returnType == null) "" else " [ $returnType ]"} { ${parameterList ?: ""} }"
	}
}