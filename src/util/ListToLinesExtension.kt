package util

fun <E> List<E>.toLines(): String {
	val builder = StringBuilder()
	for(item in this)
		builder.append("\n").append(item.toString())
	return builder.toString()
}
