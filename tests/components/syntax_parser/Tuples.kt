package components.syntax_parser

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Tuples {

	@Disabled
	@Test
	fun `parses tuple construction`() {
		val sourceCode = """
			class Player {
				var x = y = 0

				to calculateCoordinates(): (Int, Int) {
					return (x, y)
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ TypeType { class } Identifier { Player } ] { TypeBody {
					Function [ Identifier { calculateCoordinates } ParameterList {
					}: Type { ObjectType { Int }, ObjectType { Int } } ] { StatementSection { StatementBlock {
						Return {
							Tuple {
								Identifier { x }
								Identifier { y }
							}
						}
					} } }
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Disabled
	@Test
	fun `parses tuple deconstruction`() {
		val sourceCode = """
			val (x, y) = player.calculateCoordinates()
			""".trimIndent()
		val expected =
			"""
				VariableDeclaration [ val ] {
					Assignment {
						Tuple {
							Identifier { x }
							Identifier { y }
						}
						= FunctionCall [ MemberAccess {
							Identifier { player }.Identifier { calculateCoordinates }
						} ] {
						}
					}
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}
}
