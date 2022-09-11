package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.GeneratorDefinition
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.general.StatementSection
import source_structure.Position
import parsing.syntax_tree.literals.Identifier
import parsing.syntax_tree.general.TypeElement
import java.lang.StringBuilder
import java.util.*

class GeneratorDefinition(start: Position, private val identifier: Identifier, private val parameterList: ParameterList,
						  private var keyReturnType: TypeElement?, private var valueReturnType: TypeElement, private val body: StatementSection):
	Element(start, body.end) {

	override fun concretize(linter: Linter, scope: MutableScope): GeneratorDefinition {
		val parameters = LinkedList<Unit>()
		for(parameter in parameterList.parameters) {
			//TODO continue...
		}
		val generatorDefinition = GeneratorDefinition(this, identifier.getValue(), parameters,
			keyReturnType?.concretize(linter, scope), valueReturnType.concretize(linter, scope),
			body.concretize(linter, scope))
		scope.declareValue(linter, generatorDefinition)
		return generatorDefinition
	}

	override fun toString(): String {
		val string = StringBuilder()
		string.append("Generator [ ")
			.append(identifier)
			.append(" ")
			.append(parameterList)
			.append(": ")
			.append(keyReturnType ?: "void")
			.append(", ")
			.append(valueReturnType)
			.append(" ] { ")
			.append(body)
			.append(" }")
		return string.toString()
	}
}