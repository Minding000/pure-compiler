package parsing

import util.TestUtil
import org.junit.jupiter.api.Test

internal class Mutability {

	@Test
	fun `parses constant variables`() {
		val sourceCode = """
			val text = "Irreplaceable!"
			""".trimIndent()
		val expected =
			"""
				VariableSection [ val ] {
					VariableDeclaration { Identifier { text } = StringLiteral { "Irreplaceable!" } }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses immutable variables`() {
		val sourceCode = """
			immutable var id = 5
			""".trimIndent()
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { immutable } } ] {
					VariableSection [ var ] {
						VariableDeclaration { Identifier { id } = NumberLiteral { 5 } }
					}
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses immutable type definitions`() {
		val sourceCode = """
			immutable object MainMonitor {}
			""".trimIndent()
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { immutable } } ] {
					TypeDefinition [ object Identifier { MainMonitor } ] { TypeBody {
					} }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses static constants`() {
		val sourceCode = """
			class Display {
				const PERIPHERAL_TYPE = "graphics"
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Display } ] { TypeBody {
					VariableSection [ const ] {
						VariableDeclaration { Identifier { PERIPHERAL_TYPE } = StringLiteral { "graphics" } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses constant properties`() {
		val sourceCode = """
			class Display {
				val resolution: Resolution
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Display } ] { TypeBody {
					VariableSection [ val ] {
						VariableDeclaration { Identifier { resolution }: ObjectType { Identifier { Resolution } } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses immutable properties`() {
		val sourceCode = """
			class Item {
				immutable var id = 71
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Item } ] { TypeBody {
					ModifierSection [ ModifierList { Modifier { immutable } } ] {
						VariableSection [ var ] {
							VariableDeclaration { Identifier { id } = NumberLiteral { 71 } }
						}
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses mutating function definitions`() {
		val sourceCode = """
			class Human {
				var oxygenLevel = 1
				mutating to breath() {
					oxygenLevel++
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Human } ] { TypeBody {
					VariableSection [ var ] {
						VariableDeclaration { Identifier { oxygenLevel } = NumberLiteral { 1 } }
					}
					ModifierSection [ ModifierList { Modifier { mutating } } ] {
						FunctionSection [ to ] {
							Function [ Identifier { breath } ParameterList {
							}: void ] { StatementSection { StatementBlock {
								UnaryModification { Identifier { oxygenLevel }++ }
							} } }
						}
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses mutable parameters`() {
		val sourceCode = """
			class Human {
				to chargePhone(mutable phone: Phone) {
					phone.chargeInPercent += 5
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Human } ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { chargePhone } ParameterList {
							Parameter [ ModifierList { Modifier { mutable } } ] { Identifier { phone }: ObjectType { Identifier { Phone } } }
						}: void ] { StatementSection { StatementBlock {
							BinaryModification {
								MemberAccess {
									Identifier { phone }.Identifier { chargeInPercent }
								} += NumberLiteral { 5 }
							}
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}