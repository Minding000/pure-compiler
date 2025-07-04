package components.semantic_model.context

import components.semantic_model.declarations.TypeDeclaration
import components.semantic_model.types.ObjectType
import components.semantic_model.types.SelfType
import components.semantic_model.types.Type

enum class SpecialType(val className: String) {
	ARRAY("Array"),
	STRING("String"),
	BYTE("Byte"),
	BYTE_ARRAY("ByteArray"),
	INTEGER("Int"),
	FLOAT("Float"),
	BOOLEAN("Bool"),
	NULL("Null"),
	FUNCTION("Function"),
	NATIVE_INPUT_STREAM("NativeInputStream"),
	NATIVE_OUTPUT_STREAM("NativeOutputStream"),
	EXCEPTION("Exception"),
	MAP("Map"),
	ITERABLE("Iterable"),
	INDEX_ITERATOR("IndexIterator"),
	KEY_ITERATOR("KeyIterator"),
	VALUE_ITERATOR("ValueIterator"),
	NEVER("Never"),
	NOTHING("Nothing"),
	IDENTIFIABLE("Identifiable"),
	ANY("Any");

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
		if(type is SelfType)
			return matches(type.typeDeclaration)
		if(type !is ObjectType)
			return false
		return type.name == className &&
			type.getTypeDeclaration()?.scope?.enclosingScope == type.context.nativeRegistry.specialTypeScopes[this]
	}

	fun matches(typeDeclaration: TypeDeclaration?): Boolean {
		if(typeDeclaration == null)
			return false
		return typeDeclaration.name == className &&
			typeDeclaration.scope.enclosingScope == typeDeclaration.context.nativeRegistry.specialTypeScopes[this]
	}
}
