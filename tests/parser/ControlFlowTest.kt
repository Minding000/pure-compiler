package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class ControlFlowTest {

	@Test
	fun testIfStatement() {
		val sourceCode = """
			var x: Int
			if 5
				x = 3
			else
				x = 2
			""".trimIndent()
		val expected =
			"""
				Program {
					VariableDeclaration {
						TypedIdentifier { Identifier { x } : Type { Identifier { Int } } }
					}
					If [NumberLiteral { 5 }] {
						Assignment {
							Identifier { x } = NumberLiteral { 3 }
						}
					} Else {
						Assignment {
							Identifier { x } = NumberLiteral { 2 }
						}
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testConstructorCall() {
		val sourceCode = """
			class Human {
				fun speak(words: String) {
					echo words
				}
			}
			var peter = Human()
			""".trimIndent()
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Human }] { TypeBody {
						Function [Identifier { speak }(
							TypedIdentifier { Identifier { words } : Type { Identifier { String } } }
						): void] { StatementBlock {
							Print {
								Identifier { words }
							}
						} }
					} }
					VariableDeclaration {
						Assignment {
							Identifier { peter } = FunctionCall [Identifier { Human }] {
							}
						}
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testFunctionCall() {
		val sourceCode = """
			class Human {
				fun speak(words: String) {
					echo words
				}
			}
			var peter = Human()
			peter.speak("Keep up the good work!")
			""".trimIndent()
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Human }] { TypeBody {
						Function [Identifier { speak }(
							TypedIdentifier { Identifier { words } : Type { Identifier { String } } }
						): void] { StatementBlock {
							Print {
								Identifier { words }
							}
						} }
					} }
					VariableDeclaration {
						Assignment {
							Identifier { peter } = FunctionCall [Identifier { Human }] {
							}
						}
					}
					FunctionCall [ReferenceChain {
						Identifier { peter }
						Identifier { speak }
					}] {
						StringLiteral { "Keep up the good work!" }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testReturn() {
		val sourceCode = """
			class Human {
				fun speak(words: String): String {
					echo words
					return "Done"
				}
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Human }] { TypeBody {
						Function [Identifier { speak }(
							TypedIdentifier { Identifier { words } : Type { Identifier { String } } }
						): Type { Identifier { String } }] { StatementBlock {
							Print {
								Identifier { words }
							}
							Return { StringLiteral { "Done" } }
						} }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}