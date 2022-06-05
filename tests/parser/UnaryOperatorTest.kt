package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class UnaryOperatorTest {

	@Test
	fun testNot() {
		val sourceCode = "!yes"
		val expected =
			"""
				UnaryOperator { !BooleanLiteral { yes } }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testPositive() {
		val sourceCode = "+2"
		val expected =
			"""
				UnaryOperator { +NumberLiteral { 2 } }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testNegative() {
		val sourceCode = "-6"
		val expected =
			"""
				UnaryOperator { -NumberLiteral { 6 } }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testSpread() {
		val sourceCode =
			"""
				sum(...numbers)
            """.trimIndent()
		val expected =
			"""
				FunctionCall [ Identifier { sum } ] {
					UnaryOperator { ...Identifier { numbers } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testNullCheck() {
		val sourceCode = "x?"
		val expected =
			"""
				NullCheck { Identifier { x } }
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