package parsing.ast.definitions

import linter.Linter
import linter.elements.general.Unit
import linter.elements.values.TypeDefinition
import linter.scopes.Scope
import parsing.ast.general.Element
import parsing.ast.literals.Identifier
import parsing.ast.literals.Type

class GenericsListElement(private val identifier: Identifier, private val type: Type?): Element(identifier.start, type?.end ?: identifier.end) {

    override fun concretize(linter: Linter, scope: Scope): Unit {
        return TypeDefinition(this, identifier.getValue(), type?.concretize(linter, scope), true)
    }

    override fun toString(): String {
        return "GenericsListElement${if(type == null) "" else " [ $type ]"} { $identifier }"
    }
}