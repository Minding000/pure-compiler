package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class ModificationOperatorTest {

	@Test
	fun testUnaryModification() {
		val sourceCode =
			"""
				var x = 0
				x++
				x--
				echo x
            """.trimIndent()
		val expected =
			"""
				Program {
					VariableDeclaration {
						Assignment {
							Identifier { x } = NumberLiteral { 0 }
						}
					}
					UnaryModification { Identifier { x }++ }
					UnaryModification { Identifier { x }-- }
					Print {
						Identifier { x }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testBinaryModification() {
		val sourceCode =
			"""
				var x = 0
				x += 4
				x -= 3
				x *= 4
				x /= 2
				echo x
            """.trimIndent()
		val expected =
			"""
				Program {
					VariableDeclaration {
						Assignment {
							Identifier { x } = NumberLiteral { 0 }
						}
					}
					BinaryModification {
						Identifier { x } += NumberLiteral { 4 }
					}
					BinaryModification {
						Identifier { x } -= NumberLiteral { 3 }
					}
					BinaryModification {
						Identifier { x } *= NumberLiteral { 4 }
					}
					BinaryModification {
						Identifier { x } /= NumberLiteral { 2 }
					}
					Print {
						Identifier { x }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}