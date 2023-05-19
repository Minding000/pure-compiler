package components.semantic_analysis.semantic_model.context

import components.semantic_analysis.semantic_model.scopes.FileScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type

enum class SpecialType(val className: String, val pathParts: List<String> = listOf("Pure", "lang", "dataTypes", className)) {
	STRING("String"),
	INTEGER("Int"),
	FLOAT("Float"),
	BOOLEAN("Bool"),
	NULL("Null"),
	FUNCTION("Function"),
	ITERABLE("Iterable", listOf("Pure", "lang", "collections", "Iterable")),
	INDEX_ITERATOR("IndexIterator", listOf("Pure", "lang", "collections", "iterators", "IndexIterator")),
	KEY_ITERATOR("KeyIterator", listOf("Pure", "lang", "collections", "iterators", "KeyIterator")),
	VALUE_ITERATOR("ValueIterator", listOf("Pure", "lang", "collections", "iterators", "ValueIterator")),
	NEVER("Never"),
	NOTHING("Nothing"),
	ANY("Any");
	var scope: FileScope? = null

	companion object {
		fun isRootType(name: String): Boolean {
			if(name == NEVER.className)
				return true
			if(name == NOTHING.className)
				return true
			if(name == ANY.className)
				return true
			return false
		}
	}

	fun matches(type: Type?): Boolean {
		if(type !is ObjectType)
			return false
		return type.name == className && type.definition?.scope?.enclosingScope == scope
	}
}
