package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.scopes.BlockScope
import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position
import components.semantic_model.declarations.GeneratorDefinition as SemanticGeneratorDefinitionModel

class GeneratorDefinition(start: Position, private val identifier: Identifier, private val parameterList: ParameterList,
						  private var keyReturnType: TypeSyntaxTreeNode?, private var valueReturnType: TypeSyntaxTreeNode,
						  private val body: StatementSection): SyntaxTreeNode(start, body.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticGeneratorDefinitionModel {
		val generatorScope = BlockScope(scope)
		val parameters = parameterList.getSemanticParameterModels(generatorScope)
		return SemanticGeneratorDefinitionModel(this, generatorScope, identifier.getValue(), parameters,
			keyReturnType?.toSemanticModel(generatorScope), valueReturnType.toSemanticModel(generatorScope),
			body.toSemanticModel(generatorScope))
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
