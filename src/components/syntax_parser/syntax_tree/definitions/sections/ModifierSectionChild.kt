package components.syntax_parser.syntax_tree.definitions.sections

import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.MutableScope

interface ModifierSectionChild {
	var parent: ModifierSection?

	fun toSemanticModel(scope: MutableScope, semanticModels: MutableList<SemanticModel>)
}
