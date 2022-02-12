package parsing.ast.literals

import parsing.ast.Element

class SimpleType(val typeList: TypeList?, val identifier: Identifier): Element(identifier.start, typeList?.end ?: identifier.end) {

	override fun toString(): String {
		return "SimpleType { ${if(typeList == null) "" else "$typeList "}$identifier }"
	}
}