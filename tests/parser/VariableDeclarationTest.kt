package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class VariableDeclarationTest {

	@Test
	fun testVariableDeclaration() {
		val sourceCode = "var car: Int"
		val expected =
			"""
				Program {
					VariableDeclaration [ var ] {
						TypedIdentifier { Identifier { car } : Type { Identifier { Int } } }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testMultipleDeclarations() {
		val sourceCode = "var car: String, tire: Int"
		val expected =
			"""
				Program {
					VariableDeclaration [ var ] {
						TypedIdentifier { Identifier { car } : Type { Identifier { String } } }
						TypedIdentifier { Identifier { tire } : Type { Identifier { Int } } }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testAssigningDeclaration() {
		val sourceCode = "var car = 5"
		val expected =
			"""
				Program {
					VariableDeclaration [ var ] {
						Assignment {
							Identifier { car }
							= NumberLiteral { 5 }
						}
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testAssignment() {
		val sourceCode =
			"""
				var car: Int
				car = 5
            """.trimIndent()
		val expected =
			"""
				Program {
					VariableDeclaration [ var ] {
						TypedIdentifier { Identifier { car } : Type { Identifier { Int } } }
					}
					Assignment {
						Identifier { car }
						= NumberLiteral { 5 }
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}