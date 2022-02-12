package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class VariableDeclarationTest {

	@Test
	fun testVariableDeclaration() {
		val sourceCode = "var car: Int"
		val expected =
			"""
				VariableDeclaration [ var ] {
					TypedIdentifier { Identifier { car } : Type { SimpleType { Identifier { Int } } } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testMultipleDeclarations() {
		val sourceCode = "var car: String, tire: Int"
		val expected =
			"""
				VariableDeclaration [ var ] {
					TypedIdentifier { Identifier { car } : Type { SimpleType { Identifier { String } } } }
					TypedIdentifier { Identifier { tire } : Type { SimpleType { Identifier { Int } } } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testAssigningDeclaration() {
		val sourceCode = "var car = 5"
		val expected =
			"""
				VariableDeclaration [ var ] {
					Assignment {
						Identifier { car }
						= NumberLiteral { 5 }
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
				VariableDeclaration [ var ] {
					TypedIdentifier { Identifier { car } : Type { SimpleType { Identifier { Int } } } }
				}
				Assignment {
					Identifier { car }
					= NumberLiteral { 5 }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testOptionalType() {
		val sourceCode =
			"""
				var car: Int? = null
				car = 9
            """.trimIndent()
		val expected =
			"""
				VariableDeclaration [ var ] {
					Assignment {
						TypedIdentifier { Identifier { car } : Type { SimpleType { Identifier { Int } }? } }
						= NullLiteral
					}
				}
				Assignment {
					Identifier { car }
					= NumberLiteral { 9 }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testOrUnionType() {
		val sourceCode =
			"""
				var car: Int | Float
            """.trimIndent()
		val expected =
			"""
				VariableDeclaration [ var ] {
					TypedIdentifier { Identifier { car } : Type { BinaryOperator {
						SimpleType { Identifier { Int } } | SimpleType { Identifier { Float } }
					} } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testAndUnionType() {
		val sourceCode =
			"""
				var refuge: Park & NatureReserve
            """.trimIndent()
		val expected =
			"""
				VariableDeclaration [ var ] {
					TypedIdentifier { Identifier { refuge } : Type { BinaryOperator {
						SimpleType { Identifier { Park } } & SimpleType { Identifier { NatureReserve } }
					} } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}