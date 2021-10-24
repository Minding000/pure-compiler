package objects

class Line(val file: File, val number: Int, val content: String) {
    override fun toString(): String {
        return "Line-$number: $content"
    }
}