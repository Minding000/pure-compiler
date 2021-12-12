import org.junit.jupiter.api.Test

internal class LiteralTest {

	@Test
	fun testNullLiteral() {
		val sourceCode = "null"
		val expected =
			"""
				Program {
					NullLiteral
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testBooleanLiteral() {
		val sourceCode = """
				yes
				no
			""".trimIndent()
		val expected =
			"""
				Program {
					BooleanLiteral { yes }
					BooleanLiteral { no }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testNumberLiteral() {
		val sourceCode = "345"
		val expected =
			"""
				Program {
					NumberLiteral { 345 }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testStringLiteral() {
		val sourceCode = "\"hello world!\""
		val expected =
			"""
				Program {
					StringLiteral { "hello world!" }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}