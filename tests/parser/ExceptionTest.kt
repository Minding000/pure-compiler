package parser

import TestUtil
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
				} Handle [ Type { SimpleType { Identifier { NoWordsException } } } Identifier { e } ] { StatementBlock {
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
				} Handle [ Type { BinaryOperator {
					SimpleType { Identifier { NoWordsException } } | SimpleType { Identifier { OverthinkException } }
				} } ] { StatementBlock {
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
				TypeDefinition [ TypeType { class } Identifier { Human } ] { TypeBody {
					Function [ Identifier { speak } ParameterList {
						Parameter [] { TypedIdentifier { Identifier { words } : Type { SimpleType { Identifier { String } } } } }
					}: void ] { StatementSection { StatementBlock {
						Print {
							Identifier { words }
						}
					} Handle [ Type { SimpleType { Identifier { NoWordsException } } } Identifier { e } ] { StatementBlock {
					} } } }
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
				} Always { StatementBlock {
					Print {
						StringLiteral { "Stopped" }
					}
				} } }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}