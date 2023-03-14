package components.syntax_parser

import logger.Severity
import logger.issues.parsing.InvalidSyntax
import org.junit.jupiter.api.Test
import util.TestUtil

internal class General {

	@Test
	fun `ignores empty files`() {
		TestUtil.assertSameSyntaxTree("", "")
	}

	@Test
	fun `emits error for unexpected end of file`() {
		val sourceCode = """
			player.
			""".trimIndent()
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertIssueDetected<InvalidSyntax>("""
			Unexpected end of file.
			Expected IDENTIFIER instead.
		""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `recovers after syntax error in file`() {
		val sourceCode = """
			++
			player.name
			""".trimIndent()
		val expected =
			"""
				MemberAccess {
					Identifier { player }.Identifier { name }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `recovers after syntax error in statement block`() {
		val sourceCode = """
			{
				++
				player.name
			}
			""".trimIndent()
		val expected =
			"""
				StatementSection { StatementBlock {
					MemberAccess {
						Identifier { player }.Identifier { name }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
