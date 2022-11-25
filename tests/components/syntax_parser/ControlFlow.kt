package components.syntax_parser

import util.TestUtil
import org.junit.jupiter.api.Test

internal class ControlFlow {

	@Test
	fun `parses if statements`() {
		val sourceCode = """
			if 2 + 3 == 5 {
			}
			""".trimIndent()
		val expected =
			"""
				If [ BinaryOperator {
					BinaryOperator {
						NumberLiteral { 2 } + NumberLiteral { 3 }
					} == NumberLiteral { 5 }
				} ] {
					StatementSection { StatementBlock {
					} }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses if statements with else branch`() {
		val sourceCode = """
			var x: Int
			if 5
				x = 3
			else
				x = 2
			""".trimIndent()
		val expected =
			"""
				VariableSection [ var ] {
					VariableDeclaration { Identifier { x }: ObjectType { Identifier { Int } } }
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
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses initializer calls`() {
		val sourceCode = """
			Human class {
				to speak(words: String) {
					echo words
				}
			}
			var peter = Human()
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Human } class ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { speak } ParameterList {
							Parameter { Identifier { words }: ObjectType { Identifier { String } } }
						}: void ] { StatementSection { StatementBlock {
							Print {
								Identifier { words }
							}
						} } }
					}
				} }
				VariableSection [ var ] {
					VariableDeclaration { Identifier { peter } = FunctionCall [ Identifier { Human } ] {
					} }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses function calls`() {
		val sourceCode = """
			Human class {
				to speak(words: String) {
					echo words
				}
			}
			var peter = Human()
			peter.speak("Keep up the good work!")
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Human } class ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { speak } ParameterList {
							Parameter { Identifier { words }: ObjectType { Identifier { String } } }
						}: void ] { StatementSection { StatementBlock {
							Print {
								Identifier { words }
							}
						} } }
					}
				} }
				VariableSection [ var ] {
					VariableDeclaration { Identifier { peter } = FunctionCall [ Identifier { Human } ] {
					} }
				}
				FunctionCall [ MemberAccess {
					Identifier { peter }.Identifier { speak }
				} ] {
					StringLiteral { "Keep up the good work!" }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses return statements`() {
		val sourceCode = """
			Human class {
				to speak(words: String): String {
					echo words
					return "Done"
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Human } class ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { speak } ParameterList {
							Parameter { Identifier { words }: ObjectType { Identifier { String } } }
						}: ObjectType { Identifier { String } } ] { StatementSection { StatementBlock {
							Print {
								Identifier { words }
							}
							Return { StringLiteral { "Done" } }
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses switch statements with else branch`() {
		val sourceCode = """
			switch x {
				ExitCode.SUCCESS:
					echo "Success"
				else:
					echo "Failed"
			}
			""".trimIndent()
		val expected =
			"""
				Switch [ Identifier { x } ] {
					Case [ MemberAccess {
						Identifier { ExitCode }.Identifier { SUCCESS }
					} ] {
						Print {
							StringLiteral { "Success" }
						}
					}
				} Else {
					Print {
						StringLiteral { "Failed" }
					}
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses switch statements without else branch`() {
		val sourceCode = """
			switch x {
				ExitCode.SUCCESS:
					echo "Success"
				ExitCode.FAILURE:
					echo "Failure"
			}
			""".trimIndent()
		val expected =
			"""
				Switch [ Identifier { x } ] {
					Case [ MemberAccess {
						Identifier { ExitCode }.Identifier { SUCCESS }
					} ] {
						Print {
							StringLiteral { "Success" }
						}
					}
					Case [ MemberAccess {
						Identifier { ExitCode }.Identifier { FAILURE }
					} ] {
						Print {
							StringLiteral { "Failure" }
						}
					}
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
