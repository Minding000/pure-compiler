package util

import java.util.*

fun <Item> linkedListOf(vararg items: Item): LinkedList<Item> {
	val list = LinkedList<Item>()
	list.addAll(items)
	return list
}
