package parser

import TestUtil
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
				Program {
					TypeDefinition [ModifierList { Modifier { imm } } TypeType { object } Identifier { MainMonitor }] { TypeBody {
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
				Program {
					TypeDefinition [ModifierList { Modifier { imm } } TypeType { class } Identifier { Monitor }] { TypeBody {
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
				Program {
					TypeDefinition [TypeType { class } Identifier { Display }] { TypeBody {
						PropertyDeclaration [ const ] {
							Assignment {
								Identifier { PERIPHERAL_TYPE } = StringLiteral { "graphics" }
							}
						}
					} }
				}
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
				Program {
					TypeDefinition [TypeType { class } Identifier { Display }] { TypeBody {
						PropertyDeclaration [ val ] {
							TypedIdentifier { Identifier { resolution } : Type { Identifier { Resolution } } }
						}
					} }
				}
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
				Program {
					VariableDeclaration [ val ] {
						Assignment {
							Identifier { text } = StringLiteral { "Irreplaceable!" }
						}
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testImmutableFunction() {
		val sourceCode = """
			class Human {
				imm fun speak(words: String) {
					echo words
				}
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Human }] { TypeBody {
						Function [ModifierList { Modifier { imm } } Identifier { speak } ParameterList {
							Parameter [] { TypedIdentifier { Identifier { words } : Type { Identifier { String } } } }
						}: void] { StatementBlock {
							Print {
								Identifier { words }
							}
						} }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testMutableParameter() {
		val sourceCode = """
			class Human {
				fun chargePhone(mut phone: Phone) {
					phone.chargeInPercent += 5
				}
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Human }] { TypeBody {
						Function [Identifier { chargePhone } ParameterList {
							Parameter [ ModifierList { Modifier { mut } } ] { TypedIdentifier { Identifier { phone } : Type { Identifier { Phone } } } }
						}: void] { StatementBlock {
							BinaryModification {
								ReferenceChain {
									Identifier { phone }
									Identifier { chargeInPercent }
								} += NumberLiteral { 5 }
							}
						} }
					} }
				}
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
				Program {
					VariableDeclaration [ ModifierList { Modifier { imm } } var ] {
						Assignment {
							Identifier { id } = NumberLiteral { 5 }
						}
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
				Program {
					TypeDefinition [TypeType { class } Identifier { Item }] { TypeBody {
						PropertyDeclaration [ ModifierList { Modifier { imm } } val ] {
							Assignment {
								Identifier { id } = NumberLiteral { 71 }
							}
						}
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}