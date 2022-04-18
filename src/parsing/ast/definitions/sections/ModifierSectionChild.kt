package parsing.ast.definitions.sections

import linter.Linter
import linter.elements.general.Unit
import linter.scopes.Scope

interface ModifierSectionChild {
	var parent: ModifierSection?

	fun concretize(linter: Linter, scope: Scope, units: MutableList<Unit>)
}