package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class UnaryOperatorTest {

	@Test
	fun testNot() {
		val sourceCode = "!yes"
		val expected =
			"""
				Program {
					UnaryOperator { !BooleanLiteral { yes } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testPositive() {
		val sourceCode = "+2"
		val expected =
			"""
				Program {
					UnaryOperator { +NumberLiteral { 2 } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testNegative() {
		val sourceCode = "-6"
		val expected =
			"""
				Program {
					UnaryOperator { -NumberLiteral { 6 } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testSpread() {
		val sourceCode = "...parameters"
		val expected =
			"""
				Program {
					UnaryOperator { ...Identifier { parameters } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testMultipleNot() {
		val sourceCode = "!!yes"
		TestUtil.assertUserError("Unexpected NOT", sourceCode)
	}

	@Test
	fun testMultiNegative() {
		val sourceCode = "--4"
		TestUtil.assertUserError("Unexpected DECREMENT", sourceCode)
	}

	@Test
	fun testMultiPositive() {
		val sourceCode = "++8"
		TestUtil.assertUserError("Unexpected INCREMENT", sourceCode)
	}
}