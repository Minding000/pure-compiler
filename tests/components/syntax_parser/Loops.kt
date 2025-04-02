package components.syntax_parser

import org.junit.jupiter.api.Test
import util.TestUtil

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
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
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
					Break
				} } }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
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
					Next
				} } }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
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
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
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
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses pre-until-loops`() {
		val sourceCode = """
			loop until x < 5 {
				x++
			}
			""".trimIndent()
		val expected =
			"""
				Loop [ WhileGenerator [pre negated] {
					BinaryOperator {
						Identifier { x } Operator { < } NumberLiteral { 5 }
					}
				} ] { StatementSection { StatementBlock {
					UnaryModification { Identifier { x } Operator { ++ } }
				} } }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses post-until-loops`() {
		val sourceCode = """
			loop {
				x--
			} until x > 5
			""".trimIndent()
		val expected =
			"""
				Loop [ WhileGenerator [post negated] {
					BinaryOperator {
						Identifier { x } Operator { > } NumberLiteral { 5 }
					}
				} ] { StatementSection { StatementBlock {
					UnaryModification { Identifier { x } Operator { -- } }
				} } }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses over-loops without variable`() {
		val sourceCode = """
			loop over files {}
			""".trimIndent()
		val expected =
			"""
				Loop [ OverGenerator [ Identifier { files } ] {
				} ] { StatementSection { StatementBlock {
				} } }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses over-loops with iterator variable`() {
		val sourceCode = """
			loop over files using iterator {}
			""".trimIndent()
		val expected =
			"""
				Loop [ OverGenerator [ Identifier { files } using Identifier { iterator } ] {
				} ] { StatementSection { StatementBlock {
				} } }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses over-loops with value variable`() {
		val sourceCode = """
			loop over files as file {}
			""".trimIndent()
		val expected =
			"""
				Loop [ OverGenerator [ Identifier { files } ] {
					Identifier { file }
				} ] { StatementSection { StatementBlock {
				} } }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses over-loops with key-or-index and value variable`() {
		val sourceCode = """
			loop over files as name, file {}
			""".trimIndent()
		val expected =
			"""
				Loop [ OverGenerator [ Identifier { files } ] {
					Identifier { name }
					Identifier { file }
				} ] { StatementSection { StatementBlock {
				} } }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses over-loops with index, key and value variable`() {
		val sourceCode = """
			loop over files as position, name, file {
			}
			""".trimIndent()
		val expected =
			"""
				Loop [ OverGenerator [ Identifier { files } ] {
					Identifier { position }
					Identifier { name }
					Identifier { file }
				} ] { StatementSection { StatementBlock {
				} } }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}
}
