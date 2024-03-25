package components.syntax_parser

import org.junit.jupiter.api.Test
import util.TestUtil

internal class BinaryOperators {

	@Test
	fun `parses additions`() {
		val sourceCode = "345 + 1"
		val expected =
			"""
				BinaryOperator {
					NumberLiteral { 345 } Operator { + } NumberLiteral { 1 }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses subtractions`() {
		val sourceCode = "345 - 3"
		val expected =
			"""
				BinaryOperator {
					NumberLiteral { 345 } Operator { - } NumberLiteral { 3 }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses multiplications`() {
		val sourceCode = "345 * 2"
		val expected =
			"""
				BinaryOperator {
					NumberLiteral { 345 } Operator { * } NumberLiteral { 2 }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses divisions`() {
		val sourceCode = "345 / 5"
		val expected =
			"""
				BinaryOperator {
					NumberLiteral { 345 } Operator { / } NumberLiteral { 5 }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses null coalescence operators`() {
		val sourceCode = "repetitions ?? 1"
		val expected =
			"""
				BinaryOperator {
					Identifier { repetitions } Operator { ?? } NumberLiteral { 1 }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses binary boolean operators`() {
		val sourceCode = "yes and no or yes"
		val expected =
			"""
				BinaryOperator {
					BinaryOperator {
						BooleanLiteral { yes } Operator { and } BooleanLiteral { no }
					} Operator { or } BooleanLiteral { yes }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses binary number operators with correct precedence`() {
		val sourceCode = "3 + 345 * 2 - (2 + 1)"
		val expected =
			"""
				BinaryOperator {
					BinaryOperator {
						NumberLiteral { 3 } Operator { + } BinaryOperator {
							NumberLiteral { 345 } Operator { * } NumberLiteral { 2 }
						}
					} Operator { - } BinaryOperator {
						NumberLiteral { 2 } Operator { + } NumberLiteral { 1 }
					}
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses binary number and boolean operators with correct precedence`() {
		val sourceCode = "9 + 534 > 234 == no and 2 == 2"
		val expected =
			"""
				BinaryOperator {
					BinaryOperator {
						BinaryOperator {
							BinaryOperator {
								NumberLiteral { 9 } Operator { + } NumberLiteral { 534 }
							} Operator { > } NumberLiteral { 234 }
						} Operator { == } BooleanLiteral { no }
					} Operator { and } BinaryOperator {
						NumberLiteral { 2 } Operator { == } NumberLiteral { 2 }
					}
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `identity comparison takes precedence over equality comparison`() {
		val sourceCode = "yes == 2 === 3"
		val expected =
			"""
				BinaryOperator {
					BooleanLiteral { yes } Operator { == } BinaryOperator {
						NumberLiteral { 2 } Operator { === } NumberLiteral { 3 }
					}
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses less than operator when left hand side is a self reference`() {
		val sourceCode = "this < <Some>ThingElse"
		val expected =
			"""
				BinaryOperator {
					This Operator { < } TypeSpecification [ TypeList {
						ObjectType { Identifier { Some } }
					} ] { Identifier { ThingElse } }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}
}
