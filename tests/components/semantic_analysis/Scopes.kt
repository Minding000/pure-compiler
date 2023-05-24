package components.semantic_analysis

import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.general.Program
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.FileScope
import logger.Issue
import logger.issues.definition.DisallowedDeclarationType
import org.junit.jupiter.api.Test
import source_structure.*
import util.LintResult
import components.syntax_parser.syntax_tree.definitions.InitializerDefinition as InitializerDefinitionSyntaxTree
import components.syntax_parser.syntax_tree.general.Program as ProgramSyntaxTree

internal class Scopes {

	private inline fun <reified I: Issue> assertIssueDetected(message: String, setup: (Position) -> Unit) {
		val project = Project("Test")
		project.context.logger.addPhase("Test")
		val line = Line(File(Module(project, "Test"), emptyList(), "Test", ""), 0, 0, 1)
		val position = Position(0, line, 0)
		setup(position)
		val lintResult = LintResult(project.context, Program(ProgramSyntaxTree(emptyList())))
		lintResult.assertIssueDetected<I>(message)
	}

	@Test
	fun `prohibits initializer declarations in blocks`() {
		assertIssueDetected<DisallowedDeclarationType>("Initializer declarations aren't allowed in 'BlockScope'.") {
				position ->
			val syntaxTree = InitializerDefinitionSyntaxTree(position, null, null, position)
			val initializerDefinition = InitializerDefinition(syntaxTree, BlockScope(FileScope()))
			val blockScope = BlockScope(FileScope())
			blockScope.declareInitializer(initializerDefinition)
		}
	}
}
