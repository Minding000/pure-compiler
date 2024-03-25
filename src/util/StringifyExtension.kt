package util

import components.semantic_model.declarations.Parameter
import components.semantic_model.values.Value

fun Char.stringify(): String {
	if(isISOControl())
		return "\\0x${code}"
	return toString()
}

fun String.stringify(): String {
	var result = ""
	for(character in this)
		result += character.stringify()
	return result
}

@JvmName("stringifyValueTypes")
fun List<Value>.stringifyTypes(): String {
	return joinToString { value -> value.providedType.toString() }
}

@JvmName("stringifyParameterTypes")
fun List<Parameter>.stringifyTypes(): String {
	return joinToString { parameter -> parameter.type.toString() }
}
