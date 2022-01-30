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

	@Test
	fun testPreWhileLoop() {
		val sourceCode = """
			loop while x < 5 {
				x++
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					Loop [ WhileGenerator [pre] {
						BinaryOperator {
							Identifier { x } < NumberLiteral { 5 }
						}
					} ] { StatementBlock {
						UnaryModification { Identifier { x }++ }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testPostWhileLoop() {
		val sourceCode = """
			loop {
				x++
			} while x < 5
			""".trimIndent()
		val expected =
			"""
				Program {
					Loop [ WhileGenerator [post] {
						BinaryOperator {
							Identifier { x } < NumberLiteral { 5 }
						}
					} ] { StatementBlock {
						UnaryModification { Identifier { x }++ }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testLoopOver() {
		val sourceCode = """
			loop over files as file {
				x++
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					Loop [ OverGenerator {
						Identifier { files } as Identifier { file }
					} ] { StatementBlock {
						UnaryModification { Identifier { x }++ }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testLoopOverIndex() {
		val sourceCode = """
			loop over files as index, file {
				x++
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					Loop [ OverGenerator {
						Identifier { files } as Identifier { index }, Identifier { file }
					} ] { StatementBlock {
						UnaryModification { Identifier { x }++ }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}