import org.junit.jupiter.api.Test

internal class NumericalOperatorTest {

	@Test
	fun testAddition() {
		val sourceCode = "345 + 1"
		val expected =
			"""
                Program {
                	Addition { NumberLiteral { 345 } + NumberLiteral { 1 } }
                }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testSubtraction() {
		val sourceCode = "345 - 3"
		val expected =
			"""
                Program {
                	Addition { NumberLiteral { 345 } - NumberLiteral { 3 } }
                }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testMultiplication() {
		val sourceCode = "345 * 2"
		val expected =
			"""
                Program {
                	Multiplication { NumberLiteral { 345 } * NumberLiteral { 2 } }
                }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testDivision() {
		val sourceCode = "345 / 5"
		val expected =
			"""
                Program {
                	Multiplication { NumberLiteral { 345 } / NumberLiteral { 5 } }
                }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testPrecedence() {
		val sourceCode = "3 + 345 * 2 - (2 + 1)"
		val expected =
			"""
                Program {
                	Addition { Addition { NumberLiteral { 3 } + Multiplication { NumberLiteral { 345 } * NumberLiteral { 2 } } } - Addition { NumberLiteral { 2 } + NumberLiteral { 1 } } }
                }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}