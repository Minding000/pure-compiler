package components.syntax_parser

import org.junit.jupiter.api.Test
import util.TestUtil

internal class FileReferences {

	@Test
	fun `parses file references`() {
		val sourceCode = """
			referencing Pure
			""".trimIndent()
		val expected =
			"""
				FileReference {
					Identifier { Pure }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses file references with multiple path elements`() {
		val sourceCode = """
			referencing Pure.lang.dataTypes.String
			""".trimIndent()
		val expected =
			"""
				FileReference {
					Identifier { Pure }
					Identifier { lang }
					Identifier { dataTypes }
					Identifier { String }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses file references with type aliases`() {
		val sourceCode = """
			referencing Pure {
				String as Text
			}
			""".trimIndent()
		val expected =
			"""
				FileReference {
					Identifier { Pure }
					AliasBlock {
						Alias { Identifier { String } as Identifier { Text } }
					}
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}
}
