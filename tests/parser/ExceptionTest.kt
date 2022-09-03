package parser

import util.TestUtil
import org.junit.jupiter.api.Test

internal class ExceptionTest {

	@Test
	fun testRaise() {
		val sourceCode = """
			raise Error()
			""".trimIndent()
		val expected =
			"""
				Raise { FunctionCall [ Identifier { Error } ] {
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testBlockHandle() {
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
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testHandleUnion() {
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
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testFunctionHandle() {
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
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testTryOrNull() {
		val sourceCode = """
			try? splitApple()
			""".trimIndent()
		val expected =
			"""
				Try [ null ] { FunctionCall [ Identifier { splitApple } ] {
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testTryOrUncheck() {
		val sourceCode = """
			try! splitApple()
			""".trimIndent()
		val expected =
			"""
				Try [ uncheck ] { FunctionCall [ Identifier { splitApple } ] {
				} }
            """.trimIndent()

		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testAlways() {
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
		TestUtil.assertAST(expected, sourceCode)
	}
}