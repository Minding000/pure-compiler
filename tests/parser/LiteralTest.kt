package parser

import util.TestUtil
import org.junit.jupiter.api.Test

internal class LiteralTest {

	@Test
	fun testNullLiteral() {
		val sourceCode = "null"
		val expected =
			"""
				NullLiteral
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
				BooleanLiteral { yes }
				BooleanLiteral { no }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testSimpleNumberLiteral() {
		val sourceCode = "345"
		val expected =
			"""
				NumberLiteral { 345 }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testFloatingPointNumberLiteral() {
		val sourceCode = "6.5"
		val expected =
			"""
				NumberLiteral { 6.5 }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testSectionedNumberLiteral() {
		val sourceCode = "456_345"
		val expected =
			"""
				NumberLiteral { 456_345 }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testScientificNumberNotation() {
		val sourceCode = "10.4e-18"
		val expected =
			"""
				NumberLiteral { 10.4e-18 }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testStringLiteral() {
		val sourceCode = "\"hello world!\""
		val expected =
			"""
				StringLiteral { "hello world!" }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testStringWithEscapedQuote() {
		val sourceCode = "\"Hello!\\nDo you know the \\\"prue\\\" programming language?\""
		val expected =
			"""
				StringLiteral { "Hello!\nDo you know the \"prue\" programming language?" }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testTemplateString() {
		val sourceCode = "\"Hello \${user.salutation} \$username!\""
		val expected =
			"""
				StringLiteral { "Hello ${'$'}{user.salutation} ${'$'}username!" }
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
				StringLiteral { "
					These are
					multiple lines
					:)
				" }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}