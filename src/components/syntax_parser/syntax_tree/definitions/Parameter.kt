package components.syntax_parser.syntax_tree.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.GenericTypeDefinition
import components.semantic_analysis.semantic_model.definitions.Parameter as SemanticParameterModel
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.literals.Identifier
import components.syntax_parser.syntax_tree.general.TypeElement
import components.tokenizer.WordAtom

class Parameter(private val modifierList: ModifierList?, private val identifier: Identifier, private val type: TypeElement?):
    Element(modifierList?.start ?: identifier.start, identifier.end) {

    companion object {
        val ALLOWED_MODIFIER_TYPES = listOf(WordAtom.MUTABLE, WordAtom.SPREAD_GROUP)
    }

    override fun concretize(linter: Linter, scope: MutableScope): SemanticParameterModel {
        modifierList?.validate(linter, ALLOWED_MODIFIER_TYPES)
        val isMutable = modifierList?.containsModifier(WordAtom.MUTABLE) ?: false
        val hasDynamicSize = modifierList?.containsModifier(WordAtom.SPREAD_GROUP) ?: false
        val parameter = SemanticParameterModel(this, identifier.getValue(), type?.concretize(linter, scope), isMutable,
                hasDynamicSize)
        if(type != null)
            scope.declareValue(linter, parameter)
        return parameter
    }

	fun concretizeAsGenericParameter(linter: Linter, scope: MutableScope): TypeDefinition {
		val superType = type?.concretize(linter, scope)
		val typeScope = TypeScope(scope, superType?.scope)
		val genericTypeDefinition = GenericTypeDefinition(this, identifier.getValue(), typeScope, superType)
		typeScope.typeDefinition = genericTypeDefinition
		scope.declareType(linter, genericTypeDefinition)
		return genericTypeDefinition
	}

    override fun toString(): String {
        return "Parameter${if(modifierList == null) "" else " [ $modifierList ]"} { $identifier${if(type == null) "" else ": $type"} }"
    }
}
