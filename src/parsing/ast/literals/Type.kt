package parsing.ast.literals

import parsing.ast.Element

class Type(val baseType: Element, val hasDynamicQuantity: Boolean, val isOptional: Boolean, val typeList: TypeList?):
	Element(typeList?.start ?: baseType.start, baseType.end) {

	override fun toString(): String {
		return "Type { ${if(hasDynamicQuantity) "..." else ""}${if(typeList == null) "" else "$typeList "}$baseType${if(isOptional) "?" else ""} }"
	}
}