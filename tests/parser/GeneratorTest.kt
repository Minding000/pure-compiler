package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class GeneratorTest {

	@Test
	fun testRecursiveGenerator() {
		val sourceCode = """
			generate fibonacciSeries(index: Int): Int, Int {
				if(index == 1)
					return yield index, 1
				return yield index, index + this(index - 1)
			}
			""".trimIndent()
		val expected =
			"""
				Generator [ Identifier { fibonacciSeries } ParameterList {
					Parameter { TypedIdentifier { Identifier { index }: SimpleType { Identifier { Int } } } }
				}: SimpleType { Identifier { Int } }, SimpleType { Identifier { Int } } ] { StatementSection { StatementBlock {
					If [ BinaryOperator {
						Identifier { index } == NumberLiteral { 1 }
					} ] {
						Return { Yield { Identifier { index } NumberLiteral { 1 } } }
					}
					Return { Yield { Identifier { index } BinaryOperator {
						Identifier { index } + FunctionCall [ Identifier { this } ] {
							BinaryOperator {
								Identifier { index } - NumberLiteral { 1 }
							}
						}
					} } }
				} } }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testInfiniteGenerator() {
		val sourceCode = """
			generate fibonacciSeries(): Int, Int {
				var = 1 {
					index
					sum
				}
				loop {
					yield index, sum
					index++
					sum += index
				}
			}
			""".trimIndent()
		val expected =
			"""
				Generator [ Identifier { fibonacciSeries } ParameterList {
				}: SimpleType { Identifier { Int } }, SimpleType { Identifier { Int } } ] { StatementSection { StatementBlock {
					VariableSection [ var = NumberLiteral { 1 } ] {
						VariableDeclaration { Identifier { index } }
						VariableDeclaration { Identifier { sum } }
					}
					Loop { StatementSection { StatementBlock {
						Yield { Identifier { index } Identifier { sum } }
						UnaryModification { Identifier { index }++ }
						BinaryModification {
							Identifier { sum } += Identifier { index }
						}
					} } }
				} } }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testLoopUsingGenerator() {
		val sourceCode = """
			loop over fibonacciSeries() as fibonacciNumber {
				echo fibonacciNumber
			}
			""".trimIndent()
		val expected =
			"""
				Loop [ OverGenerator {
					FunctionCall [ Identifier { fibonacciSeries } ] {
					} as Identifier { fibonacciNumber }
				} ] { StatementSection { StatementBlock {
					Print {
						Identifier { fibonacciNumber }
					}
				} } }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}