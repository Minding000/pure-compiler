package components.parsing.syntax_tree.definitions

import components.linting.Linter
import components.linting.semantic_model.definitions.TypeDefinition
import components.linting.semantic_model.scopes.MutableScope
import components.linting.semantic_model.definitions.Parameter as SemanticParameterModel
import components.parsing.syntax_tree.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines
import java.util.*

class ParameterList(start: Position, end: Position, private val genericParameters: List<Parameter>?,
					private val parameters: List<Parameter>): MetaElement(start, end) {
	val containsGenericParameterList: Boolean
		get() = genericParameters != null

	fun concretizeGenerics(linter: Linter, scope: MutableScope): List<TypeDefinition>? {
		if(genericParameters == null)
			return null
		val generics = LinkedList<TypeDefinition>()
		for(genericParameter in genericParameters)
			generics.add(genericParameter.concretizeAsGenericParameter(linter, scope))
		return generics
	}

	fun concretizeParameters(linter: Linter, scope: MutableScope): List<SemanticParameterModel> {
		val parameters = LinkedList<SemanticParameterModel>()
		for(parameter in this.parameters)
			parameters.add(parameter.concretize(linter, scope))
		return parameters
	}

	override fun toString(): String {
		var stringRepresentation = "ParameterList {"
		if(genericParameters != null) {
			stringRepresentation += genericParameters.toLines().indent()
			stringRepresentation += ";"
		}
		stringRepresentation += parameters.toLines().indent()
		stringRepresentation += "\n}"
		return stringRepresentation
	}
}
