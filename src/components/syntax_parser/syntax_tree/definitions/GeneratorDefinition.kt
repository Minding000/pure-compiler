package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.GeneratorDefinition as SemanticGeneratorDefinitionModel
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.StatementSection
import components.syntax_parser.syntax_tree.general.TypeElement
import components.syntax_parser.syntax_tree.literals.Identifier
import source_structure.Position

class GeneratorDefinition(start: Position, private val identifier: Identifier, private val parameterList: ParameterList,
						  private var keyReturnType: TypeElement?, private var valueReturnType: TypeElement, private val body: StatementSection):
	Element(start, body.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticGeneratorDefinitionModel {
		val generatorScope = BlockScope(scope)
		val parameters = parameterList.concretizeParameters(linter, generatorScope)
		val generatorDefinition = SemanticGeneratorDefinitionModel(this, generatorScope, identifier.getValue(),
			parameters, keyReturnType?.concretize(linter, generatorScope),
			valueReturnType.concretize(linter, generatorScope), body.concretize(linter, generatorScope))
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
