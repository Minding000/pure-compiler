package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.tokenizer.Word
import components.tokenizer.WordAtom
import components.semantic_analysis.semantic_model.types.TypeParameter as SemanticTypeParameterModel

class TypeParameter(val type: TypeSyntaxTreeNode, val modifier: Word): TypeSyntaxTreeNode(type.start, modifier.end) {

    override fun toSemanticModel(scope: MutableScope): SemanticTypeParameterModel {
        val mode = if(modifier.type == WordAtom.CONSUMING)
            SemanticTypeParameterModel.Mode.CONSUMING
        else
            SemanticTypeParameterModel.Mode.PRODUCING
        return SemanticTypeParameterModel(this, scope, mode, type.toSemanticModel(scope))
    }

    override fun toString(): String {
        return "TypeParameter${" [ ${modifier.getValue()} ]"} { $type }"
    }
}
