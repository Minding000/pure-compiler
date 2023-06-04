package components.syntax_parser

import org.junit.jupiter.api.Test
import util.TestUtil

internal class VariableDeclarations {

	@Test
	fun `parses variable declarations with type`() {
		val sourceCode = "var car: Int"
		val expected =
			"""
				VariableSection [ var ] {
					LocalVariableDeclaration { Identifier { car }: ObjectType { Identifier { Int } } }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses variable declarations with multiple variables`() {
		val sourceCode = """
			var: Float = 0 {
				x
				y
			}
		""".trimIndent()
		val expected =
			"""
				VariableSection [ var: ObjectType { Identifier { Float } } = NumberLiteral { 0 } ] {
					LocalVariableDeclaration { Identifier { x } }
					LocalVariableDeclaration { Identifier { y } }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses variable declarations with assignment`() {
		val sourceCode = "var car = 5"
		val expected =
			"""
				VariableSection [ var ] {
					LocalVariableDeclaration { Identifier { car } = NumberLiteral { 5 } }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses assignments`() {
		val sourceCode =
			"""
				var car: Int
				car = 5
            """.trimIndent()
		val expected =
			"""
				VariableSection [ var ] {
					LocalVariableDeclaration { Identifier { car }: ObjectType { Identifier { Int } } }
				}
				Assignment {
					Identifier { car }
					= NumberLiteral { 5 }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses optional types`() {
		val sourceCode =
			"""
				var car: Int? = null
				car = 9
            """.trimIndent()
		val expected =
			"""
				VariableSection [ var ] {
					LocalVariableDeclaration { Identifier { car }: QuantifiedType { ObjectType { Identifier { Int } }? } = NullLiteral }
				}
				Assignment {
					Identifier { car }
					= NumberLiteral { 9 }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses or-union types`() {
		val sourceCode =
			"""
				var car: Int | Float
            """.trimIndent()
		val expected =
			"""
				VariableSection [ var ] {
					LocalVariableDeclaration { Identifier { car }: UnionType { ObjectType { Identifier { Int } } | ObjectType { Identifier { Float } } } }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses and-union types`() {
		val sourceCode =
			"""
				var refuge: Park & NatureReserve
            """.trimIndent()
		val expected =
			"""
				VariableSection [ var ] {
					LocalVariableDeclaration { Identifier { refuge }: UnionType { ObjectType { Identifier { Park } } & ObjectType { Identifier { NatureReserve } } } }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}
}
