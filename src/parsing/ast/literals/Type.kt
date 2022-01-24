package parsing.ast.literals

import parsing.ast.Element

class Type(val identifier: Identifier, val isOptional: Boolean, val typeList: TypeList?): Element(identifier.start, typeList?.end ?: identifier.end) {

	override fun toString(): String {
		return "Type { ${if(typeList == null) "" else "$typeList "}$identifier${if(isOptional) "?" else ""} }"
	}
}