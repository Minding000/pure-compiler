package components.syntax_parser

import util.TestUtil
import org.junit.jupiter.api.Test

internal class BinaryOperators {

	@Test
	fun `parses additions`() {
		val sourceCode = "345 + 1"
		val expected =
			"""
				BinaryOperator {
					NumberLiteral { 345 } + NumberLiteral { 1 }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses subtractions`() {
		val sourceCode = "345 - 3"
		val expected =
			"""
				BinaryOperator {
					NumberLiteral { 345 } - NumberLiteral { 3 }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses multiplications`() {
		val sourceCode = "345 * 2"
		val expected =
			"""
				BinaryOperator {
					NumberLiteral { 345 } * NumberLiteral { 2 }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses divisions`() {
		val sourceCode = "345 / 5"
		val expected =
			"""
				BinaryOperator {
					NumberLiteral { 345 } / NumberLiteral { 5 }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses null coalescence operators`() {
		val sourceCode = "repetitions ?? 1"
		val expected =
			"""
				BinaryOperator {
					Identifier { repetitions } ?? NumberLiteral { 1 }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses binary boolean operators`() {
		val sourceCode = "yes & no | yes"
		val expected =
			"""
				BinaryOperator {
					BinaryOperator {
						BooleanLiteral { yes } & BooleanLiteral { no }
					} | BooleanLiteral { yes }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses binary number operators with correct precedence`() {
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
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses binary number and boolean operators with correct precedence`() {
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
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
