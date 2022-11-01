package components.syntax_parser

import messages.Message
import util.TestUtil
import org.junit.jupiter.api.Test

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
		parseResult.assertMessageEmitted(Message.Type.ERROR, "Unexpected end of file")
	}

	@Test
	fun `recovers after syntax error`() {
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
}
