package parsing

import util.TestUtil
import org.junit.jupiter.api.Test

internal class TypeDefinitions {

	@Test
	fun `parses class definitions`() {
		val sourceCode = "class Animal {}"
		val expected =
			"""
				TypeDefinition [ class Identifier { Animal } ] { TypeBody {
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses object definitions`() {
		val sourceCode = "object Dog {}"
		val expected =
			"""
				TypeDefinition [ object Identifier { Dog } ] { TypeBody {
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses trait definitions`() {
		val sourceCode =
			"""
				trait Printable {
				}
            """.trimIndent()
		val expected =
			"""
				TypeDefinition [ trait Identifier { Printable } ] { TypeBody {
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses enum definitions`() {
		val sourceCode =
			"""
				enum DeliveryStatus {
					instances Pending, Cancelled, Delivered
				}
            """.trimIndent()
		val expected =
			"""
				TypeDefinition [ enum Identifier { DeliveryStatus } ] { TypeBody {
					InstanceList {
						Instance { Identifier { Pending } }
						Instance { Identifier { Cancelled } }
						Instance { Identifier { Delivered } }
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
		val sourceCode = "class Dog: Animal & Soulmate {}"
		val expected =
			"""
				TypeDefinition [ class Identifier { Dog } UnionType { ObjectType { Identifier { Animal } } & ObjectType { Identifier { Soulmate } } } ] { TypeBody {
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses member types`() {
		val sourceCode =
			"""
				class String {
					class Index {}
				}
            """.trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { String } ] { TypeBody {
					TypeDefinition [ class Identifier { Index } ] { TypeBody {
					} }
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses native modifiers`() {
		val sourceCode = "native class Goldfish {}"
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { native } } ] {
					TypeDefinition [ class Identifier { Goldfish } ] { TypeBody {
					} }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}