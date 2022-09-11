package parsing.syntax_tree.definitions.sections

import linting.Linter
import linting.semantic_model.general.Unit
import linting.semantic_model.scopes.MutableScope

interface ModifierSectionChild {
	var parent: ModifierSection?

	fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>)
}