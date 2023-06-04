package components.syntax_parser

import org.junit.jupiter.api.Test
import util.TestUtil

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
						NumberLiteral { 2 } Operator { + } NumberLiteral { 3 }
					} Operator { == } NumberLiteral { 5 }
				} ] {
					StatementSection { StatementBlock {
					} }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
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
					LocalVariableDeclaration { Identifier { x }: ObjectType { Identifier { Int } } }
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
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses external initializer calls`() {
		val sourceCode = """
			Human class {
				to speak(words: String) {}
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
						} } }
					}
				} }
				VariableSection [ var ] {
					LocalVariableDeclaration { Identifier { peter } = FunctionCall [ Identifier { Human } ] {
					} }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses internal initializer calls`() {
		val sourceCode = """
			Human class {
				init {
					init(5)
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Human } class ] { TypeBody {
					Initializer { StatementSection { StatementBlock {
						FunctionCall [ Init ] {
							NumberLiteral { 5 }
						}
					} } }
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses internal super initializer calls`() {
		val sourceCode = """
			Human class {
				init {
					super.init()
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Human } class ] { TypeBody {
					Initializer { StatementSection { StatementBlock {
						FunctionCall [ MemberAccess {
							Super.Init
						} ] {
						}
					} } }
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses function calls`() {
		val sourceCode = """
			Human class {
				to speak(words: String) {}
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
						} } }
					}
				} }
				VariableSection [ var ] {
					LocalVariableDeclaration { Identifier { peter } = FunctionCall [ Identifier { Human } ] {
					} }
				}
				FunctionCall [ MemberAccess {
					Identifier { peter }.Identifier { speak }
				} ] {
					StringLiteral { "Keep up the good work!" }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses return statements without value`() {
		val sourceCode = """
			Human class {
				to speak(words: String) {
					return
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Human } class ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { speak } ParameterList {
							Parameter { Identifier { words }: ObjectType { Identifier { String } } }
						}: void ] { StatementSection { StatementBlock {
							Return
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses return statements with value`() {
		val sourceCode = """
			Human class {
				to speak(words: String): String {
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
							Return { StringLiteral { "Done" } }
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses switch statements with else branch`() {
		val sourceCode = """
			switch x {
				ExitCode.SUCCESS:
					"Success"
				else:
					"Failed"
			}
			""".trimIndent()
		val expected =
			"""
				Switch [ Identifier { x } ] {
					Case [ MemberAccess {
						Identifier { ExitCode }.Identifier { SUCCESS }
					} ] {
						StringLiteral { "Success" }
					}
				} Else {
					StringLiteral { "Failed" }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses switch statements without else branch`() {
		val sourceCode = """
			switch x {
				ExitCode.SUCCESS:
					"Success"
				ExitCode.FAILURE:
					"Failure"
			}
			""".trimIndent()
		val expected =
			"""
				Switch [ Identifier { x } ] {
					Case [ MemberAccess {
						Identifier { ExitCode }.Identifier { SUCCESS }
					} ] {
						StringLiteral { "Success" }
					}
					Case [ MemberAccess {
						Identifier { ExitCode }.Identifier { FAILURE }
					} ] {
						StringLiteral { "Failure" }
					}
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}
}
