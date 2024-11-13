package components.semantic_model.context

import components.semantic_model.scopes.FileScope
import logger.issues.resolution.SpecialTypeFileNotFound
import components.semantic_model.general.Program as SemanticProgramModel
import components.syntax_parser.syntax_tree.general.Program as ProgramSyntaxTree

class SemanticModelGenerator(val context: Context) {

	fun createSemanticModel(programSyntaxTree: ProgramSyntaxTree,
							specialTypePaths: Map<SpecialType, List<String>> = emptyMap()): SemanticProgramModel {
		val logger = context.logger
		logger.addPhase("Semantic model creation")
		val semanticProgramModel = programSyntaxTree.toSemanticModel(context)
		logger.addPhase("Literal scope resolution")
		for((specialType, pathParts) in specialTypePaths)
			context.nativeRegistry.specialTypeScopes[specialType] = getSpecialTypeFileScope(semanticProgramModel, pathParts) ?: continue
		context.nativeRegistry.specialTypePaths.putAll(specialTypePaths)
		logger.addPhase("Declaration")
		semanticProgramModel.declare()
		logger.addPhase("File reference resolution")
		semanticProgramModel.resolveFileReferences()
		logger.addPhase("Type resolution")
		semanticProgramModel.determineTypes()
		logger.addPhase("Data flow analysis")
		semanticProgramModel.analyseDataFlow()
		logger.addPhase("Validation")
		semanticProgramModel.validate()
		return semanticProgramModel
	}

	private fun getSpecialTypeFileScope(semanticProgramModel: SemanticProgramModel, pathParts: List<String>): FileScope? {
		val file = semanticProgramModel.getFile(pathParts)
		if(file == null)
			context.addIssue(SpecialTypeFileNotFound(pathParts))
		return file?.scope
	}

	//TODO remove these comments when no longer needed (they currently are still relevant!):

	//TODO replace all phases between 'type linking' and 'data flow analysis' with 'determine types' including:
	// - Value.getType() - to get type (use enum: undetermined, being_determined, indeterminable, determined)
	// - Function.resolveSignatures() - before resolving functions in function call or operators
	// - TypeDefinition.resolveInitializers() - before resolving initializer in function call

	// Implementation differences:
	// - removed 'type linking' stage (because generic copies require values to be linked)				GOOD
	// - using 'hasDeterminedTypes' properties instead													GOOD
	// - created 'Initializer.determineSignatureTypes()'												GOOD
	// - created 'getLinkedType()' function in ValueDeclaration instead, replacing 'preLinkValues()'	IMPROVE
	// - called 'determineTypes()' manually in some places												IMPROVE
	// - didn't create 'Function.resolveSignatures()'													IMPROVE
}
