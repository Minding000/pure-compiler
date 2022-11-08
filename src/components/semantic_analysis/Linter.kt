package components.semantic_analysis

import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import messages.Message
import messages.MessageLogger
import components.syntax_parser.syntax_tree.general.Element
import components.semantic_analysis.semantic_model.general.Program as SemanticProgramModel
import components.syntax_parser.syntax_tree.general.Program as ProgramSyntaxTree

class Linter {
	private val literalScopes = HashMap<LiteralType, Scope?>()
	val logger = MessageLogger("linter", Message.Type.INFO)
	private var activePhase = Phase.PENDING //TODO consider removing this property if it is not needed

	fun lint(programSyntaxTree: ProgramSyntaxTree): SemanticProgramModel {
		logger.addPhase("Concretization")
		activePhase = Phase.CONCRETIZATION
		val semanticProgramModel = programSyntaxTree.concretize(this)
		logger.addPhase("File reference resolution")
		activePhase = Phase.FILE_REFERENCE_RESOLUTION
		semanticProgramModel.resolveFileReferences(this)
		logger.addPhase("Literal scope resolution")
		activePhase = Phase.LITERAL_SCOPE_RESOLUTION
		for(literalType in LiteralType.values()) {
			literalScopes[literalType] = getLiteralScope(semanticProgramModel,
				listOf("Pure", "lang", "dataTypes", literalType.className))
		}
		logger.addPhase("Type linking")
		activePhase = Phase.TYPE_LINKING
		semanticProgramModel.linkTypes(this)
		logger.addPhase("Property parameter linking")
		activePhase = Phase.PROPERTY_PARAMETER_LINKING
		semanticProgramModel.linkPropertyParameters(this)
		logger.addPhase("Resolve generics")
		activePhase = Phase.RESOLVE_GENERICS
		semanticProgramModel.resolveGenerics(this)
		logger.addPhase("Value linking")
		activePhase = Phase.VALUE_LINKING
		semanticProgramModel.linkValues(this)
		logger.addPhase("Validation")
		activePhase = Phase.VALIDATION
		semanticProgramModel.validate(this)
		logger.addPhase("Done")
		activePhase = Phase.DONE
		return semanticProgramModel
	}

	private fun getLiteralScope(semanticProgramModel: SemanticProgramModel, pathParts: List<String>): Scope? {
		val file = semanticProgramModel.getFile(pathParts)
		if(file == null)
			logger.add(Message("Failed to get literal scope '${pathParts.joinToString(".")}'.", Message.Type.ERROR))
		return file?.scope
	}

	fun addMessage(description: String, type: Message.Type = Message.Type.INFO) {
		logger.add(Message(description, type))
	}

	fun addMessage(element: Element, description: String, type: Message.Type = Message.Type.INFO) {
		addMessage("${element.getStartString()}: $description", type)
	}

	fun link(literalType: LiteralType, type: Type?) {
		literalScopes[literalType]?.let { literalScope -> type?.linkTypes(this, literalScope) }
	}

	fun hasCompleted(phase: Phase): Boolean {
		return phase < activePhase
	}

	enum class Phase {
		PENDING,
		CONCRETIZATION,
		LITERAL_SCOPE_RESOLUTION,
		FILE_REFERENCE_RESOLUTION,
		TYPE_LINKING,
		PROPERTY_PARAMETER_LINKING,
		RESOLVE_GENERICS,
		VALUE_LINKING,
		VALIDATION,
		DONE
	}

	enum class LiteralType(val className: String) {
		STRING("String"),
		NUMBER("Int"),
		BOOLEAN("Bool"),
		NULL("Null"),
		NOTHING("Nothing"),
		ANY("Any");

		fun matches(type: Type?): Boolean {
			return type.toString() == className
		}
	}
}
