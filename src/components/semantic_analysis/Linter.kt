package components.semantic_analysis

import components.semantic_analysis.semantic_model.scopes.FileScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element
import messages.Message
import messages.MessageLogger
import components.semantic_analysis.semantic_model.general.Program as SemanticProgramModel
import components.syntax_parser.syntax_tree.general.Program as ProgramSyntaxTree

class Linter {
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
		for(literalType in SpecialType.values())
			literalType.scope = getLiteralScope(semanticProgramModel, literalType.pathParts)
		logger.addPhase("Type linking")
		activePhase = Phase.TYPE_LINKING
		semanticProgramModel.linkTypes(this)
		logger.addPhase("Resolve generics")
		activePhase = Phase.RESOLVE_GENERICS
		semanticProgramModel.resolveGenerics(this)
		logger.addPhase("Property parameter linking")
		activePhase = Phase.PROPERTY_PARAMETER_LINKING
		semanticProgramModel.linkPropertyParameters(this)
		logger.addPhase("Value linking")
		activePhase = Phase.VALUE_LINKING
		semanticProgramModel.linkValues(this)
		logger.addPhase("Data flow analysis")
		activePhase = Phase.DATA_FLOW_ANALYSIS
		semanticProgramModel.analyseDataFlow(this)
		logger.addPhase("Validation")
		activePhase = Phase.VALIDATION
		semanticProgramModel.validate(this)
		logger.addPhase("Done")
		activePhase = Phase.DONE
		return semanticProgramModel
	}

	private fun getLiteralScope(semanticProgramModel: SemanticProgramModel, pathParts: List<String>): FileScope? {
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
		DATA_FLOW_ANALYSIS,
		VALIDATION,
		DONE
	}

	enum class SpecialType(val className: String, val pathParts: List<String> = listOf("Pure", "lang", "dataTypes", className)) {
		STRING("String"),
		INTEGER("Int"),
		FLOAT("Float"),
		BOOLEAN("Bool"),
		NULL("Null"),
		FUNCTION("Function"),
		ITERABLE("Iterable", listOf("Pure", "lang", "collections", "Iterable")),
		INDEX_ITERATOR("IndexIterator", listOf("Pure", "lang", "collections", "iterators", "IndexIterator")),
		KEY_ITERATOR("KeyIterator", listOf("Pure", "lang", "collections", "iterators", "KeyIterator")),
		VALUE_ITERATOR("ValueIterator", listOf("Pure", "lang", "collections", "iterators", "ValueIterator")),
		NEVER("Never"),
		NOTHING("Nothing"),
		ANY("Any");
		var scope: FileScope? = null

		companion object {
			fun isRootType(name: String): Boolean {
				if(name == NEVER.className)
					return true
				if(name == NOTHING.className)
					return true
				if(name == ANY.className)
					return true
				return false
			}
		}

		fun matches(type: Type?): Boolean {
			if(type !is ObjectType)
				return false
			return type.name == className && type.definition?.scope?.parentScope == scope
		}
	}
}
