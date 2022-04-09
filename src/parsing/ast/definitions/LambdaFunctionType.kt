package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.LambdaFunctionType
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.ast.literals.QuantifiedType
import parsing.ast.literals.Type
import source_structure.Position
import java.util.*

class LambdaFunctionType(start: Position, private val parameterList: LambdaParameterList?,
						 private val returnType: Type?, end: Position): Type(start, end) {

	override fun concretize(linter: Linter, scope: Scope): LambdaFunctionType {
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