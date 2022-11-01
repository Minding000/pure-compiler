package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.types.TypeParameter
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.TypeElement
import components.tokenizer.Word
import components.tokenizer.WordAtom

class TypeParameter(val type: TypeElement, val modifier: Word): TypeElement(type.start, modifier.end) {

    override fun concretize(linter: Linter, scope: MutableScope): TypeParameter {
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
