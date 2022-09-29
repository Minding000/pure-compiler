package linting

import linting.semantic_model.literals.Type
import parsing.syntax_tree.general.Program as ProgramSyntaxTree
import linting.semantic_model.general.Program as SemanticProgramModel
import messages.Message
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.general.Element
import java.util.*
import kotlin.collections.HashMap

class Linter {
	private val logLevel = Message.Type.DEBUG
	private val literalScopes = HashMap<LiteralType, Scope?>()
	val messages = LinkedList<Message>()
	var phase = Phase.PENDING

	fun lint(programSyntaxTree: ProgramSyntaxTree): SemanticProgramModel {
		messages.add(Message("----- Linter stage: Concretization -----", Message.Type.DEBUG))
		phase = Phase.CONCRETIZATION
		val semanticProgramModel = programSyntaxTree.concretize(this)
		messages.add(Message("----- Linter stage: File reference resolution -----", Message.Type.DEBUG))
		phase = Phase.LITERAL_SCOPE_RESOLUTION
		messages.add(Message("----- Linter stage: Literal scope resolution -----", Message.Type.DEBUG))
		for(literalType in LiteralType.values()) {
			literalScopes[literalType] = getLiteralScope(semanticProgramModel,
				listOf("Pure", "lang", "dataTypes", literalType.className))
		}
		phase = Phase.FILE_REFERENCE_RESOLUTION
		semanticProgramModel.resolveFileReferences(this)
		messages.add(Message("----- Linter stage: Type linking -----", Message.Type.DEBUG))
		phase = Phase.TYPE_LINKING
		semanticProgramModel.linkTypes(this)
		messages.add(Message("----- Linter stage: Property parameter linking -----", Message.Type.DEBUG))
		phase = Phase.PROPERTY_PARAMETER_LINKING
		semanticProgramModel.linkPropertyParameters(this)
		messages.add(Message("----- Linter stage: Value linking -----", Message.Type.DEBUG))
		phase = Phase.VALUE_LINKING
		semanticProgramModel.linkValues(this)
		messages.add(Message("----- Linter stage: Validation -----", Message.Type.DEBUG))
		phase = Phase.VALIDATION
		semanticProgramModel.validate(this)
		messages.add(Message("----- Linter stage: Done -----", Message.Type.DEBUG))
		phase = Phase.DONE
		return semanticProgramModel
	}

	private fun getLiteralScope(semanticProgramModel: SemanticProgramModel, pathParts: List<String>): Scope? {
		val file = semanticProgramModel.getFile(pathParts)
		if(file == null)
			messages.add(Message("Failed to get literal scope '${pathParts.joinToString(".")}'.", Message.Type.ERROR))
		return file?.scope
	}

	fun addMessage(description: String, type: Message.Type = Message.Type.INFO) {
		messages.add(Message(description, type))
	}

	fun addMessage(element: Element, description: String, type: Message.Type = Message.Type.INFO) {
		addMessage("${element.getStartString()}: $description", type)
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

	fun link(literalType: LiteralType, type: Type?) {
		literalScopes[literalType]?.let { literalScope -> type?.linkTypes(this, literalScope) }
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

	enum class LiteralType(val className: String) {
		STRING("String"),
		NUMBER("Int"),
		BOOLEAN("Bool"),
		NULL("Null"),
		NOTHING("Nothing");

		fun matches(type: Type?): Boolean {
			return type.toString() == className
		}
	}
}