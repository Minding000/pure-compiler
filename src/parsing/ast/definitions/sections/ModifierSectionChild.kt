package parsing.ast.definitions.sections

import linter.Linter
import linter.elements.general.Unit
import linter.scopes.MutableScope

interface ModifierSectionChild {
	var parent: ModifierSection?

	fun concretize(linter: Linter, scope: MutableScope, units: MutableList<Unit>)
}