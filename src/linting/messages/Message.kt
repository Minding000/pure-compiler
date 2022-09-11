package linting.messages

open class Message(val description: String, val type: Type = Type.INFO) {

	enum class Type {
		DEBUG,
		INFO,
		WARNING,
		ERROR
	}
}