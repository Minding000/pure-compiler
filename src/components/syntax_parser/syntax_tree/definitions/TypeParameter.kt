package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeElement
import components.tokenizer.Word
import components.tokenizer.WordAtom
import components.semantic_analysis.semantic_model.types.TypeParameter as SemanticTypeParameterModel

class TypeParameter(val type: TypeElement, val modifier: Word): TypeElement(type.start, modifier.end) {

    override fun concretize(linter: Linter, scope: MutableScope): SemanticTypeParameterModel {
        val mode = if(modifier.type == WordAtom.CONSUMING)
            SemanticTypeParameterModel.Mode.CONSUMING
        else
            SemanticTypeParameterModel.Mode.PRODUCING
        return SemanticTypeParameterModel(this, scope, mode, type.concretize(linter, scope))
    }

    override fun toString(): String {
        return "TypeParameter${" [ ${modifier.getValue()} ]"} { $type }"
    }
}
