package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class LiteralTest {

	@Test
	fun testNullLiteral() {
		val sourceCode = "null"
		val expected =
			"""
				Program {
					NullLiteral
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testBooleanLiteral() {
		val sourceCode = """
				yes
				no
			""".trimIndent()
		val expected =
			"""
				Program {
					BooleanLiteral { yes }
					BooleanLiteral { no }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testNumberLiteral() {
		val sourceCode = "345"
		val expected =
			"""
				Program {
					NumberLiteral { 345 }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testStringLiteral() {
		val sourceCode = "\"hello world!\""
		val expected =
			"""
				Program {
					StringLiteral { "hello world!" }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testStringWithEscapedQuote() {
		val sourceCode = "\"Hello!\\nDo you know the \\\"prue\\\" programming language?\""
		val expected =
			"""
				Program {
					StringLiteral { "Hello!\nDo you know the \"prue\" programming language?" }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testTemplateString() {
		val sourceCode = "\"Hello \${user.salutation} \$username!\""
		val expected =
			"""
				Program {
					StringLiteral { "Hello ${'$'}{user.salutation} ${'$'}username!" }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testMultilineString() {
		val sourceCode =
			"""
				${'"'}
					These are
					multiple lines
					:)
				${'"'}
            """.trimIndent()
		val expected =
			"""
				Program {
					StringLiteral { "
						These are
						multiple lines
						:)
					" }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}