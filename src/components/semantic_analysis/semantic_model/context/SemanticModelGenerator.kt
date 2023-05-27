package components.semantic_analysis.semantic_model.context

import components.semantic_analysis.semantic_model.scopes.FileScope
import logger.issues.resolution.LiteralFileNotFound
import components.semantic_analysis.semantic_model.general.Program as SemanticProgramModel
import components.syntax_parser.syntax_tree.general.Program as ProgramSyntaxTree

class SemanticModelGenerator(val context: Context) {
	private var activePhase = Phase.PENDING //TODO consider removing this property if it is not needed

	fun createSemanticModel(programSyntaxTree: ProgramSyntaxTree): SemanticProgramModel {
		val logger = context.logger
		logger.addPhase("Semantic model creation")
		activePhase = Phase.SEMANTIC_MODEL_CREATION
		val semanticProgramModel = programSyntaxTree.toSemanticModel()
		logger.addPhase("Literal scope resolution")
		activePhase = Phase.LITERAL_SCOPE_RESOLUTION
		for(literalType in SpecialType.values())
			literalType.scope = getLiteralScope(semanticProgramModel, literalType.pathParts)
		logger.addPhase("Declaration")
		activePhase = Phase.DECLARATION
		semanticProgramModel.declare()
		logger.addPhase("File reference resolution")
		activePhase = Phase.FILE_REFERENCE_RESOLUTION
		semanticProgramModel.resolveFileReferences()
		logger.addPhase("Determine types")
		activePhase = Phase.DETERMINE_TYPES
		semanticProgramModel.determineTypes()
		logger.addPhase("Data flow analysis")
		activePhase = Phase.DATA_FLOW_ANALYSIS
		semanticProgramModel.analyseDataFlow()
		logger.addPhase("Validation")
		activePhase = Phase.VALIDATION
		semanticProgramModel.validate()
		logger.addPhase("Done")
		activePhase = Phase.DONE
		return semanticProgramModel
	}

	private fun getLiteralScope(semanticProgramModel: SemanticProgramModel, pathParts: List<String>): FileScope? {
		val file = semanticProgramModel.getFile(pathParts)
		if(file == null)
			context.addIssue(LiteralFileNotFound(pathParts))
		return file?.scope
	}

	enum class Phase {
		PENDING,
		SEMANTIC_MODEL_CREATION,		// Create semantic model from abstract syntax tree
		LITERAL_SCOPE_RESOLUTION,		// Create references to literal type definitions
		DECLARATION,					// Declare types and values in scopes (except for initializers)
		FILE_REFERENCE_RESOLUTION,		// Resolves references between file by collecting the declared types and values
		//TODO replace all phases between 'type linking' and 'data flow analysis' with 'determine types' including:
		// - Value.getType() - to get type (use enum: undetermined, being_determined, indeterminable, determined)
		// - TypeDefinition.resolveInitializers() - before resolving initializer in function call
		// - Function.resolveSignatures() - before resolving functions in function call or operators

		// Implementation differences:
		// - removed 'type linking' stage (because generic copies require values to be linked)				GOOD
		// - using 'hasDeterminedTypes' properties instead													GOOD
		// - created 'getLinkedType()' function in ValueDeclaration instead, replacing 'preLinkValues()'	IMPROVE
		// - called 'determineTypes()' manually in some places												IMPROVE
		// - didn't create 'TypeDefinition.resolveInitializers()'											IMPROVE
		// - didn't create 'Function.resolveSignatures()'													IMPROVE - DO THIS NEXT

		DETERMINE_TYPES,				// Determine types of values that don't have explicit types
		DATA_FLOW_ANALYSIS,				// Run dataflow analysis to detect constant conditions and similar issues
		VALIDATION,						// Run validations on the final semantic model
		DONE
	}

}
