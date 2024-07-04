package util

fun String.count(substring: String): Int {
	var count = 0
	var startIndex = 0
	while(startIndex < length) {
		val index = indexOf(substring, startIndex)
		if(index < 0)
			break
		count++
		startIndex = index + substring.length
	}
	return count
}

fun String.count(character: Char): Int {
	return count { it == character }
}
