package linter

import parsing.ast.general.Program as ProgramAST
import linter.elements.general.Program
import linter.messages.Message
import java.util.*

class Linter {
	val logLevel = Message.Type.DEBUG
	val messages = LinkedList<Message>()
	var phase = Phase.PENDING

	fun lint(ast: ProgramAST): Program {
		messages.add(Message("----- Linter stage: Concretization -----", Message.Type.DEBUG))
		phase = Phase.CONCRETIZATION
		val program = ast.concretize(this)
		messages.add(Message("----- Linter stage: File reference resolution -----", Message.Type.DEBUG))
		phase = Phase.FILE_REFERENCE_RESOLUTION
		program.resolveFileReferences(this)
		messages.add(Message("----- Linter stage: Type linking -----", Message.Type.DEBUG))
		phase = Phase.TYPE_LINKING
		program.linkTypes(this)
		messages.add(Message("----- Linter stage: Type alias resolution -----", Message.Type.DEBUG))
		phase = Phase.TYPE_ALIAS_RESOLUTION
		//program.resolveTypeAliases(this)
		messages.add(Message("----- Linter stage: Property parameter linking -----", Message.Type.DEBUG))
		phase = Phase.PROPERTY_PARAMETER_LINKING
		program.linkPropertyParameters(this)
		messages.add(Message("----- Linter stage: Reference linking -----", Message.Type.DEBUG))
		phase = Phase.VALUE_LINKING
		program.linkValues(this)
		messages.add(Message("----- Linter stage: Validation -----", Message.Type.DEBUG))
		phase = Phase.VALIDATION
		program.validate(this)
		messages.add(Message("----- Linter stage: Done -----", Message.Type.DEBUG))
		phase = Phase.DONE
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

	fun hasCompleted(phase: Phase): Boolean {
		return phase < this.phase
	}

	enum class Phase {
		PENDING,
		CONCRETIZATION,
		FILE_REFERENCE_RESOLUTION,
		TYPE_LINKING,
		TYPE_ALIAS_RESOLUTION,
		PROPERTY_PARAMETER_LINKING,
		VALUE_LINKING,
		VALIDATION,
		DONE
	}
}