package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines
import java.util.*
import components.semantic_analysis.semantic_model.definitions.Parameter as SemanticParameterModel

class ParameterList(start: Position, end: Position, private val genericParameters: List<Parameter>?,
					private val parameters: List<Parameter>): MetaElement(start, end) {
	val containsGenericParameterList: Boolean
		get() = genericParameters != null
	val containsParameters: Boolean
		get() = parameters.isNotEmpty()

	fun concretizeGenerics(scope: MutableScope): List<TypeDefinition>? {
		if(genericParameters == null)
			return null
		val generics = LinkedList<TypeDefinition>()
		for(genericParameter in genericParameters)
			generics.add(genericParameter.concretizeAsGenericParameter(scope))
		return generics
	}

	fun concretizeParameters(scope: MutableScope): List<SemanticParameterModel> {
		val parameters = LinkedList<SemanticParameterModel>()
		for(parameter in this.parameters)
			parameters.add(parameter.concretize(scope))
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
