package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.LambdaFunctionDefinition
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.general.StatementSection
import parsing.ast.general.ValueElement
import source_structure.Position
import util.concretize
import java.util.*

class LambdaFunctionDefinition(start: Position, private val parameterList: ParameterList?,
							   private val body: StatementSection): ValueElement(start, body.end) {

	override fun concretize(linter: Linter, scope: Scope): LambdaFunctionDefinition {
		val parameters = parameterList?.parameters?.concretize(linter, scope) ?: LinkedList<Unit>()
		return LambdaFunctionDefinition(this, parameters, body.concretize(linter, scope))
	}

	override fun toString(): String {
		return "LambdaFunctionDefinition${if(parameterList == null) "" else " [ $parameterList ]"} { $body }"
	}
}