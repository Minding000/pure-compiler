package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class LoopTest {

	@Test
	fun testLoop() {
		val sourceCode = """
			loop {
				echo "Hello!"
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					Loop { StatementBlock {
						Print {
							StringLiteral { "Hello!" }
						}
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testBreak() {
		val sourceCode = """
			loop {
				echo "Hello!"
				break
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					Loop { StatementBlock {
						Print {
							StringLiteral { "Hello!" }
						}
						Break {  }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testNext() {
		val sourceCode = """
			loop {
				echo "Hello!"
				next
				echo "You'll never see me :("
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					Loop { StatementBlock {
						Print {
							StringLiteral { "Hello!" }
						}
						Next {  }
						Print {
							StringLiteral { "You'll never see me :(" }
						}
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}