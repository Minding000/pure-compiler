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
		for(message in messages)
			if(message.type >= logLevel)
				println(message.description)
	}
}