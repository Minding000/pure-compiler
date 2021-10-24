package objects

class Word(val type: WordType, val value: String) {

    fun getLength(): Int {
        return value.length
    }
}