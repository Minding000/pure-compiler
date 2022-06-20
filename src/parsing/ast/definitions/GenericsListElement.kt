package parsing.ast.definitions

import linter.Linter
import linter.elements.values.TypeDefinition
import linter.scopes.MutableScope
import linter.scopes.TypeScope
import parsing.ast.general.Element
import parsing.ast.literals.Identifier
import parsing.ast.general.TypeElement

class GenericsListElement(private val identifier: Identifier, private val superType: TypeElement?):
    Element(identifier.start, superType?.end ?: identifier.end) {

    override fun concretize(linter: Linter, scope: MutableScope): TypeDefinition {
        val superType = superType?.concretize(linter, scope)
        val typeScope = TypeScope(scope, superType?.scope)
        val genericType = TypeDefinition(this, identifier.getValue(), typeScope, superType, true)
        typeScope.typeDefinition = genericType
        scope.declareType(linter, genericType)
        return genericType
    }

    override fun toString(): String {
        return "GenericsListElement${if(superType == null) "" else " [ $superType ]"} { $identifier }"
    }
}