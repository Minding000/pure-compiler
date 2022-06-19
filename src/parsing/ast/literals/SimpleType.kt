package parsing.ast.literals

import linter.Linter
import linter.elements.literals.SimpleType
import linter.scopes.MutableScope
import parsing.ast.general.TypeElement
import java.util.*

class SimpleType(private val typeList: TypeList?, private val identifier: Identifier):
	TypeElement(identifier.start, typeList?.end ?: identifier.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SimpleType {
		val types = LinkedList<linter.elements.literals.Type>()
		if(typeList != null) {
			for(type in typeList.typeParameters)
				types.add(type.concretize(linter, scope))
		}
		return SimpleType(this, types, identifier.getValue())
	}

	override fun toString(): String {
		return "SimpleType { ${if(typeList == null) "" else "$typeList "}$identifier }"
	}
}