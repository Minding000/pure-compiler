package util

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