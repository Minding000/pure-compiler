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
					Declaration {
						VariableIdentifier { x }
					}
					If {
						Assignment { VariableReference { x } = NumberLiteral { 3 } }
					} Else {
						Assignment { VariableReference { x } = NumberLiteral { 2 } }
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
					Class [ClassIdentifier { Human }] {
						Function [VariableIdentifier { speak }(
							VariableIdentifier { words }
						)] {
							Print {
								VariableReference { words }
							}
						}
					}
					Declaration {
						Assignment { VariableIdentifier { peter } = FunctionCall [Human] {
						} }
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
					Class [ClassIdentifier { Human }] {
						Function [VariableIdentifier { speak }(
							VariableIdentifier { words }
						)] {
						}
					}
					FunctionCall [VariableIdentifier { speak }] {
						StringLiteral { "Keep up the good work!" }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}