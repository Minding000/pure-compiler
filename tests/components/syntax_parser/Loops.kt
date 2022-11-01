package components.syntax_parser

import util.TestUtil
import org.junit.jupiter.api.Test

internal class Loops {

	@Test
	fun `parses loops`() {
		val sourceCode = """
			loop {
				echo "Hello!"
			}
			""".trimIndent()
		val expected =
			"""
				Loop { StatementSection { StatementBlock {
					Print {
						StringLiteral { "Hello!" }
					}
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses break statements`() {
		val sourceCode = """
			loop {
				echo "Hello!"
				break
			}
			""".trimIndent()
		val expected =
			"""
				Loop { StatementSection { StatementBlock {
					Print {
						StringLiteral { "Hello!" }
					}
					Break {  }
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses next statements`() {
		val sourceCode = """
			loop {
				echo "Hello!"
				next
				echo "You'll never see me :("
			}
			""".trimIndent()
		val expected =
			"""
				Loop { StatementSection { StatementBlock {
					Print {
						StringLiteral { "Hello!" }
					}
					Next {  }
					Print {
						StringLiteral { "You'll never see me :(" }
					}
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses pre-while-loops`() {
		val sourceCode = """
			loop while x < 5 {
				x++
			}
			""".trimIndent()
		val expected =
			"""
				Loop [ WhileGenerator [pre] {
					BinaryOperator {
						Identifier { x } < NumberLiteral { 5 }
					}
				} ] { StatementSection { StatementBlock {
					UnaryModification { Identifier { x }++ }
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses post-while-loops`() {
		val sourceCode = """
			loop {
				x++
			} while x < 5
			""".trimIndent()
		val expected =
			"""
				Loop [ WhileGenerator [post] {
					BinaryOperator {
						Identifier { x } < NumberLiteral { 5 }
					}
				} ] { StatementSection { StatementBlock {
					UnaryModification { Identifier { x }++ }
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses over-loops`() {
		val sourceCode = """
			loop over files as file {
				x++
			}
			""".trimIndent()
		val expected =
			"""
				Loop [ OverGenerator {
					Identifier { files } as Identifier { file }
				} ] { StatementSection { StatementBlock {
					UnaryModification { Identifier { x }++ }
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses over-index-loops`() {
		val sourceCode = """
			loop over files as index, file {
				x++
			}
			""".trimIndent()
		val expected =
			"""
				Loop [ OverGenerator {
					Identifier { files } as Identifier { index }, Identifier { file }
				} ] { StatementSection { StatementBlock {
					UnaryModification { Identifier { x }++ }
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
