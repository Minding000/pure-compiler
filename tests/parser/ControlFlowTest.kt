package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class ControlFlowTest {

	@Test
	fun testIfStatement() {
		val sourceCode = """
			if 2 + 3 == 5 {
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					If [ BinaryOperator {
						BinaryOperator {
							NumberLiteral { 2 } + NumberLiteral { 3 }
						} == NumberLiteral { 5 }
					} ] {
						StatementBlock {
						}
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testIfElseStatement() {
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
					VariableDeclaration [ var ] {
						TypedIdentifier { Identifier { x } : Type { Identifier { Int } } }
					}
					If [ NumberLiteral { 5 } ] {
						Assignment {
							Identifier { x }
							= NumberLiteral { 3 }
						}
					} Else {
						Assignment {
							Identifier { x }
							= NumberLiteral { 2 }
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
				to speak(words: String) {
					echo words
				}
			}
			var peter = Human()
			""".trimIndent()
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Human }] { TypeBody {
						Function [Identifier { speak } ParameterList {
							Parameter [] { TypedIdentifier { Identifier { words } : Type { Identifier { String } } } }
						}: void] { StatementBlock {
							Print {
								Identifier { words }
							}
						} }
					} }
					VariableDeclaration [ var ] {
						Assignment {
							Identifier { peter }
							= FunctionCall [Identifier { Human }] {
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
				to speak(words: String) {
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
						Function [Identifier { speak } ParameterList {
							Parameter [] { TypedIdentifier { Identifier { words } : Type { Identifier { String } } } }
						}: void] { StatementBlock {
							Print {
								Identifier { words }
							}
						} }
					} }
					VariableDeclaration [ var ] {
						Assignment {
							Identifier { peter }
							= FunctionCall [Identifier { Human }] {
							}
						}
					}
					MemberAccess {
						Identifier { peter }.FunctionCall [Identifier { speak }] {
							StringLiteral { "Keep up the good work!" }
						}
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testReturn() {
		val sourceCode = """
			class Human {
				to speak(words: String): String {
					echo words
					return "Done"
				}
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Human }] { TypeBody {
						Function [Identifier { speak } ParameterList {
							Parameter [] { TypedIdentifier { Identifier { words } : Type { Identifier { String } } } }
						}: Type { Identifier { String } }] { StatementBlock {
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