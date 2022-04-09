package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.InitializerDefinition
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.ast.general.StatementSection
import source_structure.Position
import java.util.*

class InitializerDefinition(start: Position, private val parameterList: ParameterList?,
							private val body: StatementSection?, end: Position):
	Element(start, end) {

	override fun concretize(linter: Linter, scope: Scope): InitializerDefinition {
		//TODO concretize modifiers
		val parameters = LinkedList<Unit>()
		if(parameterList != null) {
			for(parameter in parameterList.parameters)
				parameters.add(parameter.concretize(linter, scope))
		}
		return InitializerDefinition(this, parameters, body?.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Initializer [ $parameterList ] { ${body ?: ""} }"
	}
}