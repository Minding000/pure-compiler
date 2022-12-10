package components.syntax_parser

import util.TestUtil
import org.junit.jupiter.api.Test

internal class Exceptions {

	@Test
	fun `parses raise statements`() {
		val sourceCode = """
			raise Error()
			""".trimIndent()
		val expected =
			"""
				Raise { FunctionCall [ Identifier { Error } ] {
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses handle blocks`() {
		val sourceCode = """
			{
			} handle NoWordsException e {
			}
			""".trimIndent()
		val expected =
			"""
				StatementSection { StatementBlock {
				} Handle [ ObjectType { Identifier { NoWordsException } } Identifier { e } ] { StatementBlock {
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses handle blocks with multiple types`() {
		val sourceCode = """
			{
			} handle NoWordsException | OverthinkException {
			}
			""".trimIndent()
		val expected =
			"""
				StatementSection { StatementBlock {
				} Handle [ UnionType { ObjectType { Identifier { NoWordsException } } | ObjectType { Identifier { OverthinkException } } } ] { StatementBlock {
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses function handle blocks`() {
		val sourceCode = """
			Human class {
				to speak(words: String) {
				} handle NoWordsException e {
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Human } class ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { speak } ParameterList {
							Parameter { Identifier { words }: ObjectType { Identifier { String } } }
						}: void ] { StatementSection { StatementBlock {
						} Handle [ ObjectType { Identifier { NoWordsException } } Identifier { e } ] { StatementBlock {
						} } } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses optional trys`() {
		val sourceCode = """
			try? splitApple()
			""".trimIndent()
		val expected =
			"""
				Try [ null ] { FunctionCall [ Identifier { splitApple } ] {
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses force trys`() {
		val sourceCode = """
			try! splitApple()
			""".trimIndent()
		val expected =
			"""
				Try [ uncheck ] { FunctionCall [ Identifier { splitApple } ] {
				} }
            """.trimIndent()

		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses always blocks`() {
		val sourceCode = """
			{
			} always {
			}
			""".trimIndent()
		val expected =
			"""
				StatementSection { StatementBlock {
				} StatementBlock {
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
