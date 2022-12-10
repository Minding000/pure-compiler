package components.syntax_parser

import util.TestUtil
import org.junit.jupiter.api.Test

internal class ModificationOperators {

	@Test
	fun `parses unary modification operators`() {
		val sourceCode =
			"""
				var x = 0
				x++
				x--
            """.trimIndent()
		val expected =
			"""
				VariableSection [ var ] {
					LocalVariableDeclaration { Identifier { x } = NumberLiteral { 0 } }
				}
				UnaryModification { Identifier { x } Operator { ++ } }
				UnaryModification { Identifier { x } Operator { -- } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses binary modification operators`() {
		val sourceCode =
			"""
				var x = 0
				x += 4
				x -= 3
				x *= 4
				x /= 2
            """.trimIndent()
		val expected =
			"""
				VariableSection [ var ] {
					LocalVariableDeclaration { Identifier { x } = NumberLiteral { 0 } }
				}
				BinaryModification {
					Identifier { x } Operator { += } NumberLiteral { 4 }
				}
				BinaryModification {
					Identifier { x } Operator { -= } NumberLiteral { 3 }
				}
				BinaryModification {
					Identifier { x } Operator { *= } NumberLiteral { 4 }
				}
				BinaryModification {
					Identifier { x } Operator { /= } NumberLiteral { 2 }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
