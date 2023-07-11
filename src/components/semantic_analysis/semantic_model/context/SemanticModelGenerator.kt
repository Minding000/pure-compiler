package components.semantic_analysis.semantic_model.context

import components.semantic_analysis.semantic_model.scopes.FileScope
import logger.issues.resolution.LiteralFileNotFound
import components.semantic_analysis.semantic_model.general.Program as SemanticProgramModel
import components.syntax_parser.syntax_tree.general.Program as ProgramSyntaxTree

class SemanticModelGenerator(val context: Context) {

	fun createSemanticModel(programSyntaxTree: ProgramSyntaxTree): SemanticProgramModel {
		val logger = context.logger
		logger.addPhase("Semantic model creation")
		val semanticProgramModel = programSyntaxTree.toSemanticModel(context)
		logger.addPhase("Literal scope resolution")
		for(literalType in SpecialType.values())
			literalType.scope = getLiteralScope(semanticProgramModel, literalType.pathParts)
		logger.addPhase("Declaration")
		semanticProgramModel.declare()
		logger.addPhase("File reference resolution")
		semanticProgramModel.resolveFileReferences()
		logger.addPhase("Determine types")
		semanticProgramModel.determineTypes()
		logger.addPhase("Data flow analysis")
		semanticProgramModel.analyseDataFlow()
		logger.addPhase("Validation")
		semanticProgramModel.validate()
		logger.addPhase("Done")
		return semanticProgramModel
	}

	private fun getLiteralScope(semanticProgramModel: SemanticProgramModel, pathParts: List<String>): FileScope? {
		val file = semanticProgramModel.getFile(pathParts)
		if(file == null)
			context.addIssue(LiteralFileNotFound(pathParts))
		return file?.scope
	}

	//TODO remove these comments:

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
	// - didn't create 'Function.resolveSignatures()'													IMPROVE
}
