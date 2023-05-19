package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.definitions.GenericTypeDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.semantic_analysis.semantic_model.types.LiteralType
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.syntax_parser.syntax_tree.general.TypeSyntaxTreeNode
import components.syntax_parser.syntax_tree.literals.Identifier
import components.tokenizer.WordAtom
import components.semantic_analysis.semantic_model.definitions.Parameter as SemanticParameterModel

class Parameter(private val modifierList: ModifierList?, private val identifier: Identifier, private val type: TypeSyntaxTreeNode?):
    SyntaxTreeNode(modifierList?.start ?: identifier.start, identifier.end) {

    companion object {
        val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.MUTABLE, WordAtom.SPREAD)
    }

    override fun toSemanticModel(scope: MutableScope): SemanticParameterModel {
        modifierList?.validate(context, ALLOWED_MODIFIER_TYPES)
        val isMutable = modifierList?.containsModifier(WordAtom.MUTABLE) ?: false
        val hasDynamicSize = modifierList?.containsModifier(WordAtom.SPREAD) ?: false
        return SemanticParameterModel(this, scope, identifier.getValue(), type?.toSemanticModel(scope), isMutable, hasDynamicSize)
    }

	fun toSemanticGenericParameterModel(scope: MutableScope): TypeDefinition {
		val superType = type?.toSemanticModel(scope) ?: LiteralType(this, scope, SpecialType.ANY)
		val typeScope = TypeScope(scope, superType.interfaceScope)
		return GenericTypeDefinition(this, identifier.getValue(), typeScope, superType)
	}

    override fun toString(): String {
        return "Parameter${if(modifierList == null) "" else " [ $modifierList ]"} { $identifier${if(type == null) "" else ": $type"} }"
    }
}
