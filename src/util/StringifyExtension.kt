package util

import linting.semantic_model.definitions.Parameter

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

fun List<Parameter>.stringify(): String {
	return joinToString { parameter -> parameter.type.toString() }
}
