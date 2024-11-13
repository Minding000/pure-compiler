package components.semantic_model.control_flow

import components.code_generation.llvm.models.control_flow.WhileGenerator
import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.Scope
import components.semantic_model.values.Value
import components.syntax_parser.syntax_tree.control_flow.WhileGenerator as WhileGeneratorSyntaxTree

class WhileGenerator(override val source: WhileGeneratorSyntaxTree, scope: Scope, val condition: Value, val isPostCondition: Boolean):
	SemanticModel(source, scope) { //TODO write post condition compilation test

	init {
		addSemanticModels(condition)
	}

	override fun toUnit() = WhileGenerator(this, condition.toUnit())
}
