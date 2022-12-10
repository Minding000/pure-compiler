package components.syntax_parser

import util.TestUtil
import org.junit.jupiter.api.Test

internal class Loops {

	@Test
	fun `parses loops`() {
		val sourceCode = """
			loop {}
			""".trimIndent()
		val expected =
			"""
				Loop { StatementSection { StatementBlock {
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses break statements`() {
		val sourceCode = """
			loop {
				break
			}
			""".trimIndent()
		val expected =
			"""
				Loop { StatementSection { StatementBlock {
					Break {  }
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses next statements`() {
		val sourceCode = """
			loop {
				next
			}
			""".trimIndent()
		val expected =
			"""
				Loop { StatementSection { StatementBlock {
					Next {  }
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
						Identifier { x } Operator { < } NumberLiteral { 5 }
					}
				} ] { StatementSection { StatementBlock {
					UnaryModification { Identifier { x } Operator { ++ } }
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses post-while-loops`() {
		val sourceCode = """
			loop {
				x--
			} while x > 5
			""".trimIndent()
		val expected =
			"""
				Loop [ WhileGenerator [post] {
					BinaryOperator {
						Identifier { x } Operator { > } NumberLiteral { 5 }
					}
				} ] { StatementSection { StatementBlock {
					UnaryModification { Identifier { x } Operator { -- } }
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
					UnaryModification { Identifier { x } Operator { ++ } }
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
					UnaryModification { Identifier { x } Operator { ++ } }
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
