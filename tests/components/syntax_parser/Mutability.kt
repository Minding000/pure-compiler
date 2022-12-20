package components.syntax_parser

import org.junit.jupiter.api.Test
import util.TestUtil

internal class Mutability {

	@Test
	fun `parses constant variables`() {
		val sourceCode = """
			val text = "Irreplaceable!"
			""".trimIndent()
		val expected =
			"""
				VariableSection [ val ] {
					LocalVariableDeclaration { Identifier { text } = StringLiteral { "Irreplaceable!" } }
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
						LocalVariableDeclaration { Identifier { id } = NumberLiteral { 5 } }
					}
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses immutable type definitions`() {
		val sourceCode = """
			immutable MainMonitor object
			""".trimIndent()
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { immutable } } ] {
					TypeDefinition [ Identifier { MainMonitor } object ] {  }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses static constants`() {
		val sourceCode = """
			Display class {
				const PERIPHERAL_TYPE = "graphics"
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Display } class ] { TypeBody {
					VariableSection [ const ] {
						PropertyDeclaration { Identifier { PERIPHERAL_TYPE } = StringLiteral { "graphics" } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses constant properties`() {
		val sourceCode = """
			Display class {
				val resolution: Resolution
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Display } class ] { TypeBody {
					VariableSection [ val ] {
						PropertyDeclaration { Identifier { resolution }: ObjectType { Identifier { Resolution } } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses immutable properties`() {
		val sourceCode = """
			Item class {
				immutable var id = 71
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Item } class ] { TypeBody {
					ModifierSection [ ModifierList { Modifier { immutable } } ] {
						VariableSection [ var ] {
							PropertyDeclaration { Identifier { id } = NumberLiteral { 71 } }
						}
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses mutating function definitions`() {
		val sourceCode = """
			Human class {
				var oxygenLevel = 1
				mutating to breath() {
					oxygenLevel++
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Human } class ] { TypeBody {
					VariableSection [ var ] {
						PropertyDeclaration { Identifier { oxygenLevel } = NumberLiteral { 1 } }
					}
					ModifierSection [ ModifierList { Modifier { mutating } } ] {
						FunctionSection [ to ] {
							Function [ Identifier { breath } ParameterList {
							}: void ] { StatementSection { StatementBlock {
								UnaryModification { Identifier { oxygenLevel } Operator { ++ } }
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
			Human class {
				to chargePhone(mutable phone: Phone) {
					phone.chargeInPercent += 5
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Human } class ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { chargePhone } ParameterList {
							Parameter [ ModifierList { Modifier { mutable } } ] { Identifier { phone }: ObjectType { Identifier { Phone } } }
						}: void ] { StatementSection { StatementBlock {
							BinaryModification {
								MemberAccess {
									Identifier { phone }.Identifier { chargeInPercent }
								} Operator { += } NumberLiteral { 5 }
							}
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
