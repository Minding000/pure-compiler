package parsing

import util.TestUtil
import org.junit.jupiter.api.Test

internal class Literals {

	@Test
	fun `parses null literals`() {
		val sourceCode = "null"
		val expected =
			"""
				NullLiteral
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses boolean literals`() {
		val sourceCode = """
				yes
				no
			""".trimIndent()
		val expected =
			"""
				BooleanLiteral { yes }
				BooleanLiteral { no }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses number literals`() {
		val sourceCode = "345"
		val expected =
			"""
				NumberLiteral { 345 }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses number literals with decimal places`() {
		val sourceCode = "6.5"
		val expected =
			"""
				NumberLiteral { 6.5 }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses number literals with thousands separators`() {
		val sourceCode = "456_345"
		val expected =
			"""
				NumberLiteral { 456_345 }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses number literals denoted using scientific notation`() {
		val sourceCode = "10.4e-18"
		val expected =
			"""
				NumberLiteral { 10.4e-18 }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses string literals`() {
		val sourceCode = "\"hello world!\""
		val expected =
			"""
				StringLiteral { "hello world!" }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses string literals with escaped quotes`() {
		val sourceCode = "\"Hello!\\nDo you know the \\\"prue\\\" programming language?\""
		val expected =
			"""
				StringLiteral { "Hello!\nDo you know the \"prue\" programming language?" }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses string literals with templates`() {
		val sourceCode = "\"Hello \${user.salutation} \$username!\""
		val expected =
			"""
				StringLiteral { "Hello ${'$'}{user.salutation} ${'$'}username!" }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses string literals over multiple lines`() {
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
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}