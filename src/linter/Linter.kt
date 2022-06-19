package linter

import parsing.ast.general.Program as ProgramAST
import linter.elements.general.Program
import linter.messages.Message
import java.util.*

class Linter {
	val logLevel = Message.Type.DEBUG
	val messages = LinkedList<Message>()

	fun lint(ast: ProgramAST): Program {
		messages.add(Message("----- Linter stage: Concretization -----", Message.Type.DEBUG))
		val program = ast.concretize(this)
		messages.add(Message("----- Linter stage: File reference resolution -----", Message.Type.DEBUG))
		program.resolveFileReferences(this)
		messages.add(Message("----- Linter stage: Type linking -----", Message.Type.DEBUG))
		program.linkTypes(this)
		messages.add(Message("----- Linter stage: Property parameter linking -----", Message.Type.DEBUG))
		program.linkPropertyParameters(this)
		messages.add(Message("----- Linter stage: Reference linking -----", Message.Type.DEBUG))
		program.linkReferences(this)
		messages.add(Message("----- Linter stage: Validation -----", Message.Type.DEBUG))
		program.validate(this)
		messages.add(Message("----- Linter stage: Done -----", Message.Type.DEBUG))
		return program
	}

	fun printMessages() {
		val counts = Array(4) { 0 }
		for(message in messages) {
			counts[message.type.ordinal]++
			if(message.type >= logLevel)
				println("${message.type.name}: ${message.description}")
		}
		println("Total: "
				+ "${counts[Message.Type.ERROR.ordinal]} errors, "
				+ "${counts[Message.Type.WARNING.ordinal]} warnings, "
				+ "${counts[Message.Type.INFO.ordinal]} infos, "
				+ "${counts[Message.Type.DEBUG.ordinal]} debug messages"
				+ " (Log level: ${logLevel.name})")
	}
}