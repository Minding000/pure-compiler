package parsing

import util.TestUtil
import org.junit.jupiter.api.Test

internal class BinaryOperatorTest {

	@Test
	fun testAddition() {
		val sourceCode = "345 + 1"
		val expected =
			"""
				BinaryOperator {
					NumberLiteral { 345 } + NumberLiteral { 1 }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testSubtraction() {
		val sourceCode = "345 - 3"
		val expected =
			"""
				BinaryOperator {
					NumberLiteral { 345 } - NumberLiteral { 3 }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testMultiplication() {
		val sourceCode = "345 * 2"
		val expected =
			"""
				BinaryOperator {
					NumberLiteral { 345 } * NumberLiteral { 2 }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testDivision() {
		val sourceCode = "345 / 5"
		val expected =
			"""
				BinaryOperator {
					NumberLiteral { 345 } / NumberLiteral { 5 }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testNullCoalescence() {
		val sourceCode = "repetitions ?? 1"
		val expected =
			"""
				BinaryOperator {
					Identifier { repetitions } ?? NumberLiteral { 1 }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testBinaryBooleanOperators() {
		val sourceCode = "yes & no | yes"
		val expected =
			"""
				BinaryOperator {
					BinaryOperator {
						BooleanLiteral { yes } & BooleanLiteral { no }
					} | BooleanLiteral { yes }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testNumberPrecedence() {
		val sourceCode = "3 + 345 * 2 - (2 + 1)"
		val expected =
			"""
				BinaryOperator {
					BinaryOperator {
						NumberLiteral { 3 } + BinaryOperator {
							NumberLiteral { 345 } * NumberLiteral { 2 }
						}
					} - BinaryOperator {
						NumberLiteral { 2 } + NumberLiteral { 1 }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testBooleanPrecedence() {
		val sourceCode = "9 + 534 > 234 == no & 2 == 2"
		val expected =
			"""
				BinaryOperator {
					BinaryOperator {
						BinaryOperator {
							BinaryOperator {
								NumberLiteral { 9 } + NumberLiteral { 534 }
							} > NumberLiteral { 234 }
						} == BooleanLiteral { no }
					} & BinaryOperator {
						NumberLiteral { 2 } == NumberLiteral { 2 }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}