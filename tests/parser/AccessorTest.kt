package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class AccessorTest {

	@Test
	fun testReferenceChain() {
		val sourceCode = """
			player.inventory
			""".trimIndent()
		val expected =
			"""
				Program {
					ReferenceChain {
						Identifier { player }
						Identifier { inventory }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testSingleIndex() {
		val sourceCode = """
			students[i]
			""".trimIndent()
		val expected =
			"""
				Program {
					Index [ Identifier { students } ] {
						Identifier { i }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testMultiIndex() {
		val sourceCode = """
			tiles[x, y]
			""".trimIndent()
		val expected =
			"""
				Program {
					Index [ Identifier { tiles } ] {
						Identifier { x }
						Identifier { y }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}