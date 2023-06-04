package components.syntax_parser

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Extensions {

	@Disabled
	@Test
	fun `parses extension declarations`() {
		val sourceCode = """
			extend String {}
			""".trimIndent()
		val expected =
			"""
				Extension [ Identifier { String } ] {
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}
}
