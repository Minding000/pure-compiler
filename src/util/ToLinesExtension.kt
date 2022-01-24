package util

import code.Main
import java.lang.StringBuilder

fun <E> List<E>.toLines(): String {
	val builder = StringBuilder()
	for(item in this)
		builder.append("\n").append(item.toString())
	return builder.toString()
}