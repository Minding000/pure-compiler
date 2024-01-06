package components.syntax_parser.syntax_tree.definitions

import components.semantic_model.general.SemanticModel
import components.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSection
import components.syntax_parser.syntax_tree.definitions.sections.ModifierSectionChild
import components.syntax_parser.syntax_tree.general.MetaSyntaxTreeNode
import components.tokenizer.WordAtom
import source_structure.Position
import util.indent
import util.toLines

class InstanceList(start: Position, private val instances: List<Instance>):
	MetaSyntaxTreeNode(start, instances.last().end), ModifierSectionChild {
	override var parent: ModifierSection? = null

	companion object {
		val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.ABSTRACT, WordAtom.OVERRIDING, WordAtom.NATIVE)
	}

	override fun toSemanticModel(scope: MutableScope, semanticModels: MutableList<SemanticModel>) {
		parent?.validate(ALLOWED_MODIFIER_TYPES)
		val isAbstract = parent?.containsModifier(WordAtom.ABSTRACT) ?: false
		val isOverriding = parent?.containsModifier(WordAtom.OVERRIDING) ?: false
		val isNative = parent?.containsModifier(WordAtom.NATIVE) ?: false
		for(instance in instances) {
			instance.isAbstract = isAbstract
			instance.isOverriding = isOverriding
			instance.isNative = isNative
			instance.toSemanticModel(scope, semanticModels)
		}
	}

	override fun toString(): String {
		return "InstanceList {${instances.toLines().indent()}\n}"
	}
}
