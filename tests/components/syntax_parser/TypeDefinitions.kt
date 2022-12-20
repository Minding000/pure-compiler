package components.syntax_parser

import org.junit.jupiter.api.Test
import util.TestUtil

internal class TypeDefinitions {

	@Test
	fun `parses class definitions without body`() {
		val sourceCode = "Animal class"
		val expected =
			"""
				TypeDefinition [ Identifier { Animal } class ] {  }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses class definitions with body`() {
		val sourceCode = "Animal class {}"
		val expected =
			"""
				TypeDefinition [ Identifier { Animal } class ] { TypeBody {
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses object definitions without body`() {
		val sourceCode = "Dog object"
		val expected =
			"""
				TypeDefinition [ Identifier { Dog } object ] {  }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses object definitions with body`() {
		val sourceCode = "Dog object {}"
		val expected =
			"""
				TypeDefinition [ Identifier { Dog } object ] { TypeBody {
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses enum definitions`() {
		val sourceCode =
			"""
				DeliveryStatus enum {
					instances Pending, Cancelled, Delivered
				}
            """.trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { DeliveryStatus } enum ] { TypeBody {
					InstanceList {
						Instance [ Identifier { Pending } ] {
						}
						Instance [ Identifier { Cancelled } ] {
						}
						Instance [ Identifier { Delivered } ] {
						}
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses instances with initializer parameters`() {
		val sourceCode =
			"""
				Color enum {
					instances Red(255, 0, 0), Green(0, 255, 0), Blue(0, 0, 255)
				}
            """.trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Color } enum ] { TypeBody {
					InstanceList {
						Instance [ Identifier { Red } ] {
							NumberLiteral { 255 }
							NumberLiteral { 0 }
							NumberLiteral { 0 }
						}
						Instance [ Identifier { Green } ] {
							NumberLiteral { 0 }
							NumberLiteral { 255 }
							NumberLiteral { 0 }
						}
						Instance [ Identifier { Blue } ] {
							NumberLiteral { 0 }
							NumberLiteral { 0 }
							NumberLiteral { 255 }
						}
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses type aliases`() {
		val sourceCode =
			"""
				alias EventHandler = (Event) =>|
            """.trimIndent()
		val expected =
			"""
				TypeAlias [ Identifier { EventHandler } ] { FunctionType { ParameterTypeList {
					ObjectType { Identifier { Event } }
				} } }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses type definitions with inheritance`() {
		val sourceCode = "Dog class: Animal & Soulmate {}"
		val expected =
			"""
				TypeDefinition [ Identifier { Dog } class UnionType { ObjectType { Identifier { Animal } } & ObjectType { Identifier { Soulmate } } } ] { TypeBody {
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses member types`() {
		val sourceCode =
			"""
				String class {
					Index class
				}
            """.trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { String } class ] { TypeBody {
					TypeDefinition [ Identifier { Index } class ] {  }
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses abstract modifiers`() {
		val sourceCode = "abstract Goldfish class"
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { abstract } } ] {
					TypeDefinition [ Identifier { Goldfish } class ] {  }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses native modifiers`() {
		val sourceCode = "native Goldfish class"
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { native } } ] {
					TypeDefinition [ Identifier { Goldfish } class ] {  }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
