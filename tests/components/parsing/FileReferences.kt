package components.parsing

import util.TestUtil
import org.junit.jupiter.api.Test

internal class FileReferences {

	@Test
	fun `parses file references`() {
		val sourceCode = """
			referencing pure
			""".trimIndent()
		val expected =
			"""
				FileReference {
					Identifier { pure }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses file references with multiple path elements`() {
		val sourceCode = """
			referencing pure.lang.dataTypes.String
			""".trimIndent()
		val expected =
			"""
				FileReference {
					Identifier { pure }
					Identifier { lang }
					Identifier { dataTypes }
					Identifier { String }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses file references with type aliases`() {
		val sourceCode = """
			referencing pure {
				String as Text
			}
			""".trimIndent()
		val expected =
			"""
				FileReference {
					Identifier { pure }
					AliasBlock {
						Alias { Identifier { String } as Identifier { Text } }
					}
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
