package components.parsing.syntax_tree.definitions

import components.linting.Linter
import components.linting.semantic_model.types.TypeParameter as SemanticTypeParameterModel
import components.linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.TypeElement
import components.tokenizer.Word
import components.tokenizer.WordAtom

class TypeParameter(val type: TypeElement, val modifier: Word): TypeElement(type.start, modifier.end) {

    override fun concretize(linter: Linter, scope: MutableScope): SemanticTypeParameterModel {
        val mode = if(modifier.type == WordAtom.CONSUMING)
            SemanticTypeParameterModel.Mode.CONSUMING
        else
            SemanticTypeParameterModel.Mode.PRODUCING
        return SemanticTypeParameterModel(this, mode, type.concretize(linter, scope))
    }

    override fun toString(): String {
        return "TypeParameter${" [ ${modifier.getValue()} ]"} { $type }"
    }
}
