package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class VariableDeclarationTest {

	@Test
	fun testVariableDeclaration() {
		val sourceCode = "var car: Int"
		val expected =
			"""
				VariableSection [ var ] {
					VariableDeclaration { Identifier { car }: SimpleType { Identifier { Int } } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testMultipleDeclarations() {
		val sourceCode = """
			var: Float = 0 {
				x
				y
			}
		""".trimIndent()
		val expected =
			"""
				VariableSection [ var: SimpleType { Identifier { Float } } = NumberLiteral { 0 } ] {
					VariableDeclaration { Identifier { x } }
					VariableDeclaration { Identifier { y } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testAssigningDeclaration() {
		val sourceCode = "var car = 5"
		val expected =
			"""
				VariableSection [ var ] {
					VariableDeclaration { Identifier { car } = NumberLiteral { 5 } }
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
				VariableSection [ var ] {
					VariableDeclaration { Identifier { car }: SimpleType { Identifier { Int } } }
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
				VariableSection [ var ] {
					VariableDeclaration { Identifier { car }: QuantifiedType { SimpleType { Identifier { Int } }? } = NullLiteral }
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
				VariableSection [ var ] {
					VariableDeclaration { Identifier { car }: UnionType { SimpleType { Identifier { Int } } | SimpleType { Identifier { Float } } } }
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
				VariableSection [ var ] {
					VariableDeclaration { Identifier { refuge }: UnionType { SimpleType { Identifier { Park } } & SimpleType { Identifier { NatureReserve } } } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}