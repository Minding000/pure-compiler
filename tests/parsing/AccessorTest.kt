package parsing

import util.TestUtil
import org.junit.jupiter.api.Test

internal class AccessorTest {

	@Test
	fun testReferenceChain() {
		val sourceCode = """
			player.inventory
			""".trimIndent()
		val expected =
			"""
				MemberAccess {
					Identifier { player }.Identifier { inventory }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testOptionalChaining() {
		val sourceCode = """
			teammate?.inventory
			""".trimIndent()
		val expected =
			"""
				MemberAccess {
					Identifier { teammate }?.Identifier { inventory }
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
				Index [ Identifier { students } ] {
					Identifier { i }
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
				Index [ Identifier { tiles } ] {
					Identifier { x }
					Identifier { y }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testInstanceAccess() {
		val sourceCode = """
			disk.state = .READY
			""".trimIndent()
		val expected =
			"""
				Assignment {
					MemberAccess {
						Identifier { disk }.Identifier { state }
					}
					= InstanceAccess { Identifier { READY } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}