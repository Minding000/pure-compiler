package util

fun String.indent(): String {
	return replace("\n", "\n\t")
}