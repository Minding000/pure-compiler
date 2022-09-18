package parsing

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
				echo words
			} handle NoWordsException e {
			
			}
			""".trimIndent()
		val expected =
			"""
				StatementSection { StatementBlock {
					Print {
						Identifier { words }
					}
				} Handle [ ObjectType { Identifier { NoWordsException } } Identifier { e } ] { StatementBlock {
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses handle blocks with multiple types`() {
		val sourceCode = """
			{
				echo words
			} handle NoWordsException | OverthinkException {
			
			}
			""".trimIndent()
		val expected =
			"""
				StatementSection { StatementBlock {
					Print {
						Identifier { words }
					}
				} Handle [ UnionType { ObjectType { Identifier { NoWordsException } } | ObjectType { Identifier { OverthinkException } } } ] { StatementBlock {
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses function handle blocks`() {
		val sourceCode = """
			class Human {
				to speak(words: String) {
					echo words
				} handle NoWordsException e {
				
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Human } ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { speak } ParameterList {
							Parameter { Identifier { words }: ObjectType { Identifier { String } } }
						}: void ] { StatementSection { StatementBlock {
							Print {
								Identifier { words }
							}
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
				echo words
			} always {
				echo "Stopped"
			}
			""".trimIndent()
		val expected =
			"""
				StatementSection { StatementBlock {
					Print {
						Identifier { words }
					}
				} StatementBlock {
					Print {
						StringLiteral { "Stopped" }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}