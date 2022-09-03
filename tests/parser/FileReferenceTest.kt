package parser

import util.TestUtil
import org.junit.jupiter.api.Test

internal class FileReferenceTest {

	@Test
	fun testSimpleReference() {
		val sourceCode = """
			referencing pure
			""".trimIndent()
		val expected =
			"""
				FileReference {
					Identifier { pure }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testReferenceChain() {
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
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testReferenceAlias() {
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
		TestUtil.assertAST(expected, sourceCode)
	}
}