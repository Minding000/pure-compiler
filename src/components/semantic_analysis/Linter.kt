package components.semantic_analysis

import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.definitions.TypeAlias
import components.semantic_analysis.semantic_model.scopes.FileScope
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.types.Type
import logger.Issue
import logger.Logger
import logger.Severity
import logger.issues.definition.CircularTypeAlias
import logger.issues.initialization.CircularAssignment
import logger.issues.resolution.LiteralFileNotFound
import java.util.*
import components.semantic_analysis.semantic_model.general.Program as SemanticProgramModel
import components.syntax_parser.syntax_tree.general.Program as ProgramSyntaxTree

class Linter {
	val logger = Logger("linter", Severity.INFO)
	val declarationStack = DeclarationStack()
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
		logger.addPhase("Declaration")
		activePhase = Phase.DECLARATION
		semanticProgramModel.declare(this)
		logger.addPhase("Determine types")
		activePhase = Phase.DETERMINE_TYPES
		semanticProgramModel.determineTypes(this)
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
			addIssue(LiteralFileNotFound(pathParts))
		return file?.scope
	}

	fun addIssue(issue: Issue) = logger.add(issue)

	enum class Phase {
		PENDING,
		CONCRETIZATION,					// Create semantic model from abstract syntax tree
		LITERAL_SCOPE_RESOLUTION,		// Create references to literal type definitions
		FILE_REFERENCE_RESOLUTION,		// Create references between files
		DECLARATION,					// Declare types and values in scopes (except for initializers)
		//TODO replace all phases between 'type linking' and 'data flow analysis' with 'determine types' including:
		// - Value.getType() - to get type (use enum: undetermined, being_determined, indeterminable, determined)
		// - TypeDefinition.resolveInitializers() - before resolving initializer in function call
		// - Function.resolveSignatures() - before resolving functions in function call or operators

		// Implementation differences:
		// - removed 'type linking' stage
		// - created 'getType()' function in ValueDeclaration instead, replacing 'preLinkValues()'
		// - created enum in 'Value', but not really using it
		// - using 'hasDeterminedTypes' properties instead
		// - didn't create 'TypeDefinition.resolveInitializers()'
		// - didn't create 'Function.resolveSignatures()'
		// - called 'determineTypes()' manually in some places

		DETERMINE_TYPES,				// Determine types of values that don't have explicit types
		DATA_FLOW_ANALYSIS,				// Run dataflow analysis to detect constant conditions and similar issues
		VALIDATION,						// Run validations on the final semantic model
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
			return type.name == className && type.definition?.scope?.enclosingScope == scope
		}
	}

	inner class DeclarationStack {
		private val typeAliases = LinkedList<TypeAlias>()
		private val propertyDeclarations = LinkedList<PropertyDeclaration>()

		fun push(typeAlias: TypeAlias): Boolean {
			if(typeAliases.contains(typeAlias)) {
				var isPartOfCircularAssignment = false
				for(existingTypeAlias in typeAliases) {
					if(!isPartOfCircularAssignment) {
						if(existingTypeAlias == typeAlias)
							isPartOfCircularAssignment = true
						else
							continue
					}
					addIssue(CircularTypeAlias(existingTypeAlias))
				}
				return false
			}
			typeAliases.add(typeAlias)
			return true
		}

		fun pop(typeAlias: TypeAlias) {
			typeAliases.remove(typeAlias)
		}

		fun push(propertyDeclaration: PropertyDeclaration): Boolean {
			if(propertyDeclarations.contains(propertyDeclaration)) {
				var isPartOfCircularAssignment = false
				for(existingPropertyDeclaration in propertyDeclarations) {
					if(!isPartOfCircularAssignment) {
						if(existingPropertyDeclaration == propertyDeclaration)
							isPartOfCircularAssignment = true
						else
							continue
					}
					addIssue(CircularAssignment(existingPropertyDeclaration))
				}
				return false
			}
			propertyDeclarations.add(propertyDeclaration)
			return true
		}

		fun pop(propertyDeclaration: PropertyDeclaration) {
			propertyDeclarations.remove(propertyDeclaration)
		}
	}
}
