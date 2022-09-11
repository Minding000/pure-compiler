package parsing

import util.TestUtil
import org.junit.jupiter.api.Test

internal class MutabilityTest {

	@Test
	fun testImmutableObject() {
		val sourceCode = """
			imm object MainMonitor {
			}
			""".trimIndent()
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { imm } } ] {
					TypeDefinition [ object Identifier { MainMonitor } ] { TypeBody {
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testImmutableClass() {
		val sourceCode = """
			imm class Monitor {
			}
			""".trimIndent()
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { imm } } ] {
					TypeDefinition [ class Identifier { Monitor } ] { TypeBody {
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testClassConstant() {
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
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testConstantProperty() {
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
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testConstantVariable() {
		val sourceCode = """
			val text = "Irreplaceable!"
			""".trimIndent()
		val expected =
			"""
				VariableSection [ val ] {
					VariableDeclaration { Identifier { text } = StringLiteral { "Irreplaceable!" } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testImmutableFunction() {
		val sourceCode = """
			class Human {
				imm to speak(words: String) {
					echo words
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Human } ] { TypeBody {
					ModifierSection [ ModifierList { Modifier { imm } } ] {
						FunctionSection [ to ] {
							Function [ Identifier { speak } ParameterList {
								Parameter { Identifier { words }: ObjectType { Identifier { String } } }
							}: void ] { StatementSection { StatementBlock {
								Print {
									Identifier { words }
								}
							} } }
						}
					}
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testMutableParameter() {
		val sourceCode = """
			class Human {
				to chargePhone(mut phone: Phone) {
					phone.chargeInPercent += 5
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Human } ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { chargePhone } ParameterList {
							Parameter [ ModifierList { Modifier { mut } } ] { Identifier { phone }: ObjectType { Identifier { Phone } } }
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
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testImmutableVariable() {
		val sourceCode = """
			imm var id = 5
			""".trimIndent()
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { imm } } ] {
					VariableSection [ var ] {
						VariableDeclaration { Identifier { id } = NumberLiteral { 5 } }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testImmutableProperty() {
		val sourceCode = """
			class Item {
				imm val id = 71
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Item } ] { TypeBody {
					ModifierSection [ ModifierList { Modifier { imm } } ] {
						VariableSection [ val ] {
							VariableDeclaration { Identifier { id } = NumberLiteral { 71 } }
						}
					}
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}