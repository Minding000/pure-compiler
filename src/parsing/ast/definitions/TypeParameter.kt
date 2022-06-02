package parsing.ast.definitions

import linter.Linter
import linter.elements.literals.TypeParameter
import linter.scopes.Scope
import parsing.ast.general.TypeElement
import parsing.tokenizer.Word
import parsing.tokenizer.WordAtom

class TypeParameter(val type: TypeElement, val modifier: Word): TypeElement(type.start, modifier.end) {

    override fun concretize(linter: Linter, scope: Scope): TypeParameter {
        val mode = if(modifier.type == WordAtom.CONSUMING)
            TypeParameter.Mode.CONSUMING
        else
            TypeParameter.Mode.PRODUCING
        return TypeParameter(this, mode, type.concretize(linter, scope))
    }

    override fun toString(): String {
        return "TypeParameter${" [ ${modifier.getValue()} ]"} { $type }"
    }
}