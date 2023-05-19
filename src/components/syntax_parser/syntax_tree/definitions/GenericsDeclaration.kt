package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.general.Unit
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.MetaElement
import source_structure.Position
import util.indent
import util.toLines

class GenericsDeclaration(start: Position, private val elements: List<Parameter>): MetaElement(start, elements.last().end) {

	override fun toSemanticModel(scope: MutableScope, units: MutableList<Unit>) {
		for(element in elements)
			units.add(element.toSemanticGenericParameterModel(scope))
	}

	override fun toString(): String {
		return "GenericsDeclaration {${elements.toLines().indent()}\n}"
	}
}
