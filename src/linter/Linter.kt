package linter

import linter.elements.general.Program
import linter.messages.Message
import java.util.*

class Linter {
	val logLevel = Message.Type.INFO
	val messages = LinkedList<Message>()

	fun lint(ast: parsing.ast.general.Program): Program {
		val program = ast.concretize(this)
		program.resolveFileReferences(this)
		program.linkTypes(this)
		program.linkReferences(this)
		program.validate(this)
		return program
	}

	fun printMessages() {
		val counts = Array<Byte>(4) { 0 }
		for(message in messages) {
			counts[message.type.ordinal]++
			if(message.type >= logLevel)
				println("${message.type.name}: ${message.description}")
		}
		println("Total: "
				+ "${counts[Message.Type.ERROR.ordinal]} errors, "
				+ "${counts[Message.Type.WARNING.ordinal]} warnings, "
				+ "${counts[Message.Type.INFO.ordinal]} infos, "
				+ "${counts[Message.Type.DEBUG.ordinal]} debug"
				+ " (Log level: ${logLevel.name})")
	}
}