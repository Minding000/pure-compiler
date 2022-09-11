package linter

import parsing.ast.general.Program as ProgramAST
import linter.elements.general.Program
import linter.messages.Message
import linter.scopes.Scope
import java.util.*

class Linter {
	val logLevel = Message.Type.DEBUG
	val messages = LinkedList<Message>()
	var phase = Phase.PENDING

	var stringLiteralScope: Scope? = null
	var numberLiteralScope: Scope? = null
	var booleanLiteralScope: Scope? = null
	var nullLiteralScope: Scope? = null
	var nothingLiteralScope: Scope? = null

	fun lint(ast: ProgramAST): Program {
		messages.add(Message("----- Linter stage: Concretization -----", Message.Type.DEBUG))
		phase = Phase.CONCRETIZATION
		val program = ast.concretize(this)
		messages.add(Message("----- Linter stage: File reference resolution -----", Message.Type.DEBUG))
		phase = Phase.LITERAL_SCOPE_RESOLUTION
		messages.add(Message("----- Linter stage: Literal scope resolution -----", Message.Type.DEBUG))
		stringLiteralScope = getLiteralScope(program, listOf("Pure", "lang", "dataTypes", "String"))
		numberLiteralScope = getLiteralScope(program, listOf("Pure", "lang", "dataTypes", "Int"))
		booleanLiteralScope = getLiteralScope(program, listOf("Pure", "lang", "dataTypes", "Bool"))
		nullLiteralScope = getLiteralScope(program, listOf("Pure", "lang", "dataTypes", "Null"))
		nothingLiteralScope = getLiteralScope(program, listOf("Pure", "lang", "dataTypes", "Nothing"))
		phase = Phase.FILE_REFERENCE_RESOLUTION
		program.resolveFileReferences(this)
		messages.add(Message("----- Linter stage: Type linking -----", Message.Type.DEBUG))
		phase = Phase.TYPE_LINKING
		program.linkTypes(this)
		messages.add(Message("----- Linter stage: Property parameter linking -----", Message.Type.DEBUG))
		phase = Phase.PROPERTY_PARAMETER_LINKING
		program.linkPropertyParameters(this)
		messages.add(Message("----- Linter stage: Value linking -----", Message.Type.DEBUG))
		phase = Phase.VALUE_LINKING
		program.linkValues(this)
		messages.add(Message("----- Linter stage: Validation -----", Message.Type.DEBUG))
		phase = Phase.VALIDATION
		program.validate(this)
		messages.add(Message("----- Linter stage: Done -----", Message.Type.DEBUG))
		phase = Phase.DONE
		return program
	}

	private fun getLiteralScope(program: Program, pathParts: List<String>): Scope? {
		val file = program.getFile(pathParts)
		if(file == null)
			messages.add(Message("Failed to get literal scope '${pathParts.joinToString(".")}'.", Message.Type.ERROR))
		return file?.scope
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
		LITERAL_SCOPE_RESOLUTION,
		FILE_REFERENCE_RESOLUTION,
		TYPE_LINKING,
		PROPERTY_PARAMETER_LINKING,
		VALUE_LINKING,
		VALIDATION,
		DONE
	}

	object Literals {
		const val STRING = "String"
		const val NUMBER = "Int"
		const val BOOLEAN = "Bool"
		const val NULL = "Null"
		const val NOTHING = "Nothing"
	}
}