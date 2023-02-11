package components.syntax_parser

import org.junit.jupiter.api.Test
import util.TestUtil

internal class Accessors {

	@Test
	fun `parses self reference`() {
		val sourceCode = """
			this
			""".trimIndent()
		val expected =
			"""
				This
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses self reference with specifier`() {
		val sourceCode = """
			this<Level>
			""".trimIndent()
		val expected =
			"""
				This [ ObjectType { Identifier { Level } } ]
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses super reference`() {
		val sourceCode = """
			super
			""".trimIndent()
		val expected =
			"""
				Super
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses super reference with specifier`() {
		val sourceCode = """
			super<Number>
			""".trimIndent()
		val expected =
			"""
				Super [ ObjectType { Identifier { Number } } ]
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses member accesses`() {
		val sourceCode = """
			player.inventory
			""".trimIndent()
		val expected =
			"""
				MemberAccess {
					Identifier { player }.Identifier { inventory }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses member type accesses in types`() {
		val sourceCode = """
			val editorTheme: Editor.Theme
			""".trimIndent()
		val expected =
			"""
				VariableSection [ val ] {
					LocalVariableDeclaration { Identifier { editorTheme }: ObjectType [
						ObjectType { Identifier { Editor } }
					] { Identifier { Theme } } }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses member type accesses with generics in the front in types`() {
		val sourceCode = """
			val integerListIterator: <Int>List.Iterator
			""".trimIndent()
		val expected =
			"""
				VariableSection [ val ] {
					LocalVariableDeclaration { Identifier { integerListIterator }: ObjectType [
						ObjectType { TypeList {
							ObjectType { Identifier { Int } }
						} Identifier { List } }
					] { Identifier { Iterator } } }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses member type accesses with generics in the middle in types`() {
		val sourceCode = """
			val brickStack: Inventory.<Brick>Stack
			""".trimIndent()
		val expected =
			"""
				VariableSection [ val ] {
					LocalVariableDeclaration { Identifier { brickStack }: ObjectType [
						ObjectType { Identifier { Inventory } }
					] { TypeList {
						ObjectType { Identifier { Brick } }
					} Identifier { Stack } } }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses member type accesses in initializer calls`() {
		val sourceCode = """
			val editorTheme = Editor.Theme()
			""".trimIndent()
		val expected =
			"""
				VariableSection [ val ] {
					LocalVariableDeclaration { Identifier { editorTheme } = FunctionCall [ MemberAccess {
						Identifier { Editor }.Identifier { Theme }
					} ] {
					} }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses member type accesses with generics in the front in initializer calls`() {
		val sourceCode = """
			val integerListIterator = <Int>List.Iterator()
			""".trimIndent()
		val expected =
			"""
				VariableSection [ val ] {
					LocalVariableDeclaration { Identifier { integerListIterator } = FunctionCall [ MemberAccess {
						TypeSpecification [ TypeList {
							ObjectType { Identifier { Int } }
						} ] { Identifier { List } }.Identifier { Iterator }
					} ] {
					} }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses member type accesses with generics in the middle in initializer calls`() {
		val sourceCode = """
			val brickStack = Inventory.<Brick>Stack()
			""".trimIndent()
		val expected =
			"""
				VariableSection [ val ] {
					LocalVariableDeclaration { Identifier { brickStack } = FunctionCall [ MemberAccess {
						Identifier { Inventory }.TypeSpecification [ TypeList {
							ObjectType { Identifier { Brick } }
						} ] { Identifier { Stack } }
					} ] {
					} }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses optional member accesses`() {
		val sourceCode = """
			teammate?.inventory
			""".trimIndent()
		val expected =
			"""
				MemberAccess {
					Identifier { teammate }?.Identifier { inventory }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses index access with one parameter`() {
		val sourceCode = """
			students[i]
			""".trimIndent()
		val expected =
			"""
				Index [ Identifier { students } ] {
					Identifier { i }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses index access with multiple parameters`() {
		val sourceCode = """
			tiles[x, y]
			""".trimIndent()
		val expected =
			"""
				Index [ Identifier { tiles } ] {
					Identifier { x }
					Identifier { y }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses instance access`() {
		val sourceCode = """
			disk.state = .READY
			""".trimIndent()
		val expected =
			"""
				Assignment {
					MemberAccess {
						Identifier { disk }.Identifier { state }
					}
					= InstanceAccess { Identifier { READY } }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
