package components.semantic_analysis

import components.semantic_analysis.semantic_model.definitions.Class
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.general.Program
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.FileScope
import components.semantic_analysis.semantic_model.scopes.TypeScope
import components.syntax_parser.syntax_tree.definitions.FunctionDefinition
import components.syntax_parser.syntax_tree.definitions.ParameterList
import components.syntax_parser.syntax_tree.definitions.TypeBody
import components.syntax_parser.syntax_tree.definitions.TypeDefinition
import components.syntax_parser.syntax_tree.literals.Identifier
import components.tokenizer.Word
import components.tokenizer.WordAtom
import logger.Issue
import logger.issues.definition.DisallowedDeclarationType
import org.junit.jupiter.api.Test
import source_structure.File
import source_structure.Line
import source_structure.Module
import source_structure.Position
import util.LintResult
import components.syntax_parser.syntax_tree.definitions.InitializerDefinition as InitializerDefinitionSyntaxTree
import components.syntax_parser.syntax_tree.general.Program as ProgramSyntaxTree

internal class Scopes {

	private inline fun <reified I: Issue> assertIssueDetected(message: String, setup: (Linter, Position) -> Unit) {
		val linter = Linter()
		linter.logger.addPhase("Test")
		val line = Line(File(Module("Test"), listOf(), "Test", ""), 0, 0, 1)
		val position = Position(0, line, 0)
		setup(linter, position)
		val lintResult = LintResult(linter, Program(ProgramSyntaxTree(listOf())))
		lintResult.assertIssueDetected<I>(message)
	}

	private fun getTestClass(position: Position): Class {
		val source = TypeDefinition(Identifier(Word(position, position, WordAtom.IDENTIFIER)),
			Word(position, position, WordAtom.CLASS), null, null, TypeBody(position, position, listOf()))
		val scope = TypeScope(FileScope(), null)
		return Class(source, "", scope, null, null, isAbstract = false, isBound = false, isNative = false,
			isMutable = false)
	}

	@Test
	fun `prohibits initializer declarations in blocks`() {
		assertIssueDetected<DisallowedDeclarationType>("Initializer declarations aren't allowed in 'BlockScope'.") {
				linter, position ->
			val syntaxTree = InitializerDefinitionSyntaxTree(position, null, null, position)
			val initializerDefinition = InitializerDefinition(
				syntaxTree, getTestClass(position), BlockScope(FileScope()), listOf(), listOf(),
				null, false
			)
			val blockScope = BlockScope(FileScope())
			blockScope.declareInitializer(linter, initializerDefinition)
		}
	}

	@Test
	fun `prohibits function declarations in blocks`() {
		assertIssueDetected<DisallowedDeclarationType>("Function declarations aren't allowed in 'BlockScope'.") {
				linter, position ->
			val syntaxTree = FunctionDefinition(Identifier(Word(position, position, WordAtom.IDENTIFIER)),
				ParameterList(position, position, null, listOf()), null, null)
			val functionImplementation = FunctionImplementation(syntaxTree, getTestClass(position),
				BlockScope(FileScope()), listOf(), listOf(), null, null)
			val blockScope = BlockScope(FileScope())
			blockScope.declareFunction(linter, "", functionImplementation)
		}
	}

//	@Test
//	fun `prohibits operator declarations in blocks`() {
//		assertErrorEmitted("Operator declarations aren't allowed in 'BlockScope'.") { linter, position ->
//			val syntaxTree = OperatorDefinitionSyntaxTree(Operator(position, position), null, null,
//				null)
//			val operatorDefinition = OperatorDefinition(syntaxTree, OperatorDefinition.Kind.PLUS,
//				BlockScope(FileScope()), listOf(), null, null, false, false,
//				false)
//			val blockScope = BlockScope(FileScope())
//			blockScope.declareOperator(linter, operatorDefinition)
//		}
//	}
}
