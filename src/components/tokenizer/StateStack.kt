package components.tokenizer

import util.linkedListOf

class StateStack {
	val list = linkedListOf(0)
	val isInString: Boolean
		get() = list.size % 2 == 0
	val openBraceCount: Int
		get() = list.last()

	fun push() {
		list.add(0)
	}

	fun pop() {
		if(list.size == 1)
			return
		list.removeLast()
	}

	fun incrementOpenBraceCount() {
		list[list.lastIndex] = list.last() + 1
	}

	fun decrementOpenBraceCount() {
		list[list.lastIndex] = list.last() - 1
	}
}
