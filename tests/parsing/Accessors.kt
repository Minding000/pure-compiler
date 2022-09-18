package parsing

import util.TestUtil
import org.junit.jupiter.api.Test

internal class Accessors {

	@Test
	fun `parses member accesses`() {
		val sourceCode = """
			player.inventory
			""".trimIndent()
		val expected =
			"""
				MemberAccess {
					Identifier { player }.Identifier { inventory }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses optional member accesses`() {
		val sourceCode = """
			teammate?.inventory
			""".trimIndent()
		val expected =
			"""
				MemberAccess {
					Identifier { teammate }?.Identifier { inventory }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses index access with one parameter`() {
		val sourceCode = """
			students[i]
			""".trimIndent()
		val expected =
			"""
				Index [ Identifier { students } ] {
					Identifier { i }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses index access with multiple parameters`() {
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
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses instance access`() {
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
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}