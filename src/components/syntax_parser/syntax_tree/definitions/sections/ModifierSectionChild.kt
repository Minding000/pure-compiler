package components.syntax_parser.syntax_tree.definitions.sections

import components.linting.Linter
import components.linting.semantic_model.general.Unit
import components.linting.semantic_model.scopes.MutableScope

interface ModifierSectionChild {
	var parent: ModifierSection?

	fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>)
}
