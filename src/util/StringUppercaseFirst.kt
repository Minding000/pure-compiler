package util

fun String.uppercaseFirstChar(): String {
	return replaceFirstChar { char -> char.uppercase() }
}
