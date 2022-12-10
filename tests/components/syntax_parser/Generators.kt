package components.syntax_parser

import util.TestUtil
import org.junit.jupiter.api.Test

internal class Generators {

	@Test
	fun `parses recursive generators`() {
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
					Parameter { Identifier { index }: ObjectType { Identifier { Int } } }
				}: ObjectType { Identifier { Int } }, ObjectType { Identifier { Int } } ] { StatementSection { StatementBlock {
					If [ BinaryOperator {
						Identifier { index } Operator { == } NumberLiteral { 1 }
					} ] {
						Return { Yield { Identifier { index } NumberLiteral { 1 } } }
					}
					Return { Yield { Identifier { index } BinaryOperator {
						Identifier { index } Operator { + } FunctionCall [ This ] {
							BinaryOperator {
								Identifier { index } Operator { - } NumberLiteral { 1 }
							}
						}
					} } }
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses infinite generators`() {
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
				}: ObjectType { Identifier { Int } }, ObjectType { Identifier { Int } } ] { StatementSection { StatementBlock {
					VariableSection [ var = NumberLiteral { 1 } ] {
						LocalVariableDeclaration { Identifier { index } }
						LocalVariableDeclaration { Identifier { sum } }
					}
					Loop { StatementSection { StatementBlock {
						Yield { Identifier { index } Identifier { sum } }
						UnaryModification { Identifier { index } Operator { ++ } }
						BinaryModification {
							Identifier { sum } Operator { += } Identifier { index }
						}
					} } }
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses loops over generators`() {
		val sourceCode = """
			loop over fibonacciSeries() as fibonacciNumber {}
			""".trimIndent()
		val expected =
			"""
				Loop [ OverGenerator {
					FunctionCall [ Identifier { fibonacciSeries } ] {
					} as Identifier { fibonacciNumber }
				} ] { StatementSection { StatementBlock {
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
