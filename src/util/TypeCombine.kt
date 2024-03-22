package util

import components.semantic_model.context.SpecialType
import components.semantic_model.general.SemanticModel
import components.semantic_model.types.OptionalType
import components.semantic_model.types.OrUnionType
import components.semantic_model.types.Type
import errors.internal.CompilerError
import java.util.*

@JvmName("combineOrUnionOptional")
fun List<Type?>.combineOrUnion(context: SemanticModel): Type? {
	val types = filterNotNull()
	if(types.isEmpty())
		return null
	return types.combineOrUnion(context)
}

fun List<Type>.combineOrUnion(context: SemanticModel): Type {
	if(isEmpty())
		throw CompilerError(context.source, "Cannot combine empty list of types.")
	val simplifiedFlattenedTypes = LinkedList<Type>()
	for(type in this) {
		val simplifiedType = type.simplified()
		if(simplifiedType is OrUnionType) {
			for(part in simplifiedType.types)
				simplifiedFlattenedTypes.add(part)
			continue
		}
		simplifiedFlattenedTypes.add(type)
	}
	val simplifiedUniqueTypes = HashSet<Type>()
	uniqueTypeSearch@for(type in simplifiedFlattenedTypes) {
		for(otherType in simplifiedFlattenedTypes) {
			if(otherType == type)
				continue
			if(otherType.accepts(type))
				continue@uniqueTypeSearch
		}
		simplifiedUniqueTypes.add(type)
	}
	if(simplifiedUniqueTypes.size == 1)
		return simplifiedUniqueTypes.first()
	val isOptional = simplifiedUniqueTypes.removeIf { type -> SpecialType.NULL.matches(type) }
	var simplifiedType = if(simplifiedUniqueTypes.size == 1)
		simplifiedUniqueTypes.first()
	else
		OrUnionType(context.source, context.scope, simplifiedUniqueTypes.toList())
	if(isOptional)
		simplifiedType = OptionalType(context.source, context.scope, simplifiedType)
	return simplifiedType
}
