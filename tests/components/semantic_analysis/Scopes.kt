package components.semantic_analysis

import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.OperatorDefinition
import components.semantic_analysis.semantic_model.general.Program
import components.semantic_analysis.semantic_model.scopes.BlockScope
import components.semantic_analysis.semantic_model.scopes.FileScope
import components.syntax_parser.syntax_tree.definitions.FunctionDefinition
import components.syntax_parser.syntax_tree.definitions.Operator
import components.syntax_parser.syntax_tree.definitions.ParameterList
import components.syntax_parser.syntax_tree.literals.Identifier
import components.tokenizer.Word
import components.tokenizer.WordAtom
import messages.Message
import components.syntax_parser.syntax_tree.definitions.InitializerDefinition as InitializerDefinitionSyntaxTree
import components.syntax_parser.syntax_tree.definitions.OperatorDefinition as OperatorDefinitionSyntaxTree
import components.syntax_parser.syntax_tree.general.Program as ProgramSyntaxTree
import org.junit.jupiter.api.Test
import source_structure.File
import source_structure.Line
import source_structure.Module
import source_structure.Position
import util.LintResult

class Scopes {

	private fun assertErrorEmitted(message: String, setup: (Linter, Position) -> Unit) {
		val linter = Linter()
		linter.logger.addPhase("Test")
		val line = Line(File(Module("Test"), listOf(), "Test", ""), 0, 0, 1)
		val position = Position(0, line, 0)
		setup(linter, position)
		val lintResult = LintResult(linter, Program(ProgramSyntaxTree(listOf())))
		lintResult.assertMessageEmitted(Message.Type.ERROR, message)
	}

	@Test
	fun `prohibits initializer declarations in blocks`() {
		assertErrorEmitted("Initializer declarations aren't allowed in 'BlockScope'.") { linter, position ->
			val syntaxTree = InitializerDefinitionSyntaxTree(position, null, null, position)
			val initializerDefinition = InitializerDefinition(
				syntaxTree, BlockScope(FileScope()), listOf(), listOf(),
				null, false
			)
			val blockScope = BlockScope(FileScope())
			blockScope.declareInitializer(linter, initializerDefinition)
		}
	}

	@Test
	fun `prohibits function declarations in blocks`() {
		assertErrorEmitted("Function declarations aren't allowed in 'BlockScope'.") { linter, position ->
			val syntaxTree = FunctionDefinition(Identifier(Word(position, position, WordAtom.IDENTIFIER)),
				ParameterList(position, position, null, listOf()), null, null)
			val functionImplementation = FunctionImplementation(syntaxTree, BlockScope(FileScope()), listOf(),
				listOf(), null, null)
			val blockScope = BlockScope(FileScope())
			blockScope.declareFunction(linter, "", functionImplementation)
		}
	}

	@Test
	fun `prohibits operator declarations in blocks`() {
		assertErrorEmitted("Operator declarations aren't allowed in 'BlockScope'.") { linter, position ->
			val syntaxTree = OperatorDefinitionSyntaxTree(Operator(position, position), null, null,
				null)
			val operatorDefinition = OperatorDefinition(syntaxTree, "", BlockScope(FileScope()), listOf(),
				null, null, false, false)
			val blockScope = BlockScope(FileScope())
			blockScope.declareOperator(linter, operatorDefinition)
		}
	}
}
