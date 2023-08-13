package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.declarations.TypeDeclaration
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.MetaSyntaxTreeNode
import source_structure.Position
import util.indent
import util.toLines
import java.util.*
import components.semantic_analysis.semantic_model.declarations.Parameter as SemanticParameterModel

class ParameterList(start: Position, end: Position, private val genericParameters: List<Parameter>?,
					private val parameters: List<Parameter>): MetaSyntaxTreeNode(start, end) {
	val containsGenericParameterList: Boolean
		get() = genericParameters != null
	val containsParameters: Boolean
		get() = parameters.isNotEmpty()

	fun getSemanticGenericParameterModels(scope: MutableScope): List<TypeDeclaration>? {
		if(genericParameters == null)
			return null
		val genericTypeDeclarations = LinkedList<TypeDeclaration>()
		for(genericParameter in genericParameters)
			genericTypeDeclarations.add(genericParameter.toSemanticGenericParameterModel(scope))
		return genericTypeDeclarations
	}

	fun getSemanticParameterModels(scope: MutableScope): List<SemanticParameterModel> {
		val parameters = LinkedList<SemanticParameterModel>()
		for(parameter in this.parameters)
			parameters.add(parameter.toSemanticModel(scope))
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
