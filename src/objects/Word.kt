package objects

class Word(val line: Line, val start: Int, val type: WordType, val value: String) {

    fun getLength(): Int {
        return value.length
    }
}