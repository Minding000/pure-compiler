package parsing.syntax_tree.definitions

import linting.Linter
import linting.semantic_model.definitions.GenericTypeDefinition
import linting.semantic_model.values.TypeDefinition
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.scopes.TypeScope
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.literals.Identifier
import parsing.syntax_tree.general.TypeElement

class GenericsListElement(private val identifier: Identifier, private val superType: TypeElement?):
    Element(identifier.start, superType?.end ?: identifier.end) {

    override fun concretize(linter: Linter, scope: MutableScope): TypeDefinition {
        val superType = superType?.concretize(linter, scope)
        val typeScope = TypeScope(scope, superType?.scope)
        val genericTypeDefinition = GenericTypeDefinition(this, identifier.getValue(), typeScope, superType)
        typeScope.typeDefinition = genericTypeDefinition
        scope.declareType(linter, genericTypeDefinition)
        return genericTypeDefinition
    }

    override fun toString(): String {
        return "GenericsListElement${if(superType == null) "" else " [ $superType ]"} { $identifier }"
    }
}