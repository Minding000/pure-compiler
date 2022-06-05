package parsing.ast.definitions

import linter.Linter
import linter.elements.definitions.Parameter
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.ast.literals.Identifier
import parsing.ast.general.TypeElement
import parsing.tokenizer.WordAtom

class Parameter(private val modifierList: ModifierList?, private val identifier: Identifier, private val type: TypeElement?):
    Element(modifierList?.start ?: identifier.start, identifier.end) {

    companion object {
        val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.MUTABLE, WordAtom.SPREAD_GROUP)
    }

    fun getTypeName(): String {
        return type?.getValue() ?: ""
    }

    override fun concretize(linter: Linter, scope: Scope): Parameter {
        modifierList?.validate(linter, ALLOWED_MODIFIER_TYPES)
        val isMutable = modifierList?.contains(WordAtom.MUTABLE) ?: false
        val hasDynamicSize = modifierList?.contains(WordAtom.SPREAD_GROUP) ?: false
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