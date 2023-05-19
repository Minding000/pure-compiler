package components.syntax_parser.syntax_tree.definitions.sections

import components.semantic_analysis.semantic_model.general.SemanticModel
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.Modifier
import components.syntax_parser.syntax_tree.definitions.ModifierList
import components.syntax_parser.syntax_tree.definitions.ModifierSpecification
import components.syntax_parser.syntax_tree.general.Element
import components.tokenizer.WordAtom
import errors.internal.CompilerError
import source_structure.Position
import util.indent
import util.toLines

class ModifierSection(private val modifierList: ModifierList, private val sections: List<Element>,
					  end: Position): DeclarationSection(modifierList.start, end), ModifierSpecification {

	init {
		for(section in sections) {
			if(section !is ModifierSectionChild)
				throw CompilerError(this, "Element not allowed in modifier section: $section")
			section.parent = this
		}
	}

	override fun toSemanticModel(scope: MutableScope, semanticModels: MutableList<SemanticModel>) {
		for(section in sections)
			section.toSemanticModel(scope, semanticModels)
	}

	override fun getOwnModifiers(): List<Modifier> {
		return modifierList.getModifiers()
	}

	override fun validate(allowedModifiers: List<WordAtom>) {
		super<ModifierSpecification>.validate(context, allowedModifiers)
	}

	override fun containsModifier(modifier: WordAtom): Boolean {
		return super<ModifierSpecification>.containsModifier(modifier)
	}

	override fun toString(): String {
		val string = StringBuilder()
		string
			.append("ModifierSection [ ")
			.append(modifierList)
			.append(" ] {")
			.append(sections.toLines().indent())
			.append("\n}")
		return string.toString()
	}
}
