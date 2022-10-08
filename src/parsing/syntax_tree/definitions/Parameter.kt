package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.Parameter
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.literals.Identifier
import parsing.syntax_tree.general.TypeElement
import parsing.tokenizer.WordAtom

class Parameter(private val modifierList: ModifierList?, private val identifier: Identifier, private val type: TypeElement?):
    Element(modifierList?.start ?: identifier.start, identifier.end) {

    companion object {
        val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.MUTABLE, WordAtom.SPREAD_GROUP)
    }

    override fun concretize(linter: Linter, scope: MutableScope): Parameter {
        modifierList?.validate(linter, ALLOWED_MODIFIER_TYPES)
        val isMutable = modifierList?.containsModifier(WordAtom.MUTABLE) ?: false
        val hasDynamicSize = modifierList?.containsModifier(WordAtom.SPREAD_GROUP) ?: false
        val parameter = Parameter(this, identifier.getValue(), type?.concretize(linter, scope), isMutable,
                hasDynamicSize)
        if(type != null)
            scope.declareValue(linter, parameter)
        return parameter
    }

    override fun toString(): String {
        return "Parameter${if(modifierList == null) "" else " [ $modifierList ]"} { $identifier${if(type == null) "" else ": $type"} }"
    }
}