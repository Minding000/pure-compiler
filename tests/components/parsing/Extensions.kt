package components.parsing

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
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
