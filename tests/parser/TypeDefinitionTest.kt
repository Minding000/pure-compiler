package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class TypeDefinitionTest {

	@Test
	fun testClassDeclaration() {
		val sourceCode = "class Animal {}"
		val expected =
			"""
				TypeDefinition [ class Identifier { Animal } ] { TypeBody {
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testObjectDeclaration() {
		val sourceCode = "object Dog {}"
		val expected =
			"""
				TypeDefinition [ object Identifier { Dog } ] { TypeBody {
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testNativeDeclaration() {
		val sourceCode = "native class Goldfish {}"
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { native } } ] {
					TypeDefinition [ class Identifier { Goldfish } ] { TypeBody {
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testInheritance() {
		val sourceCode = "class Dog: Animal & Soulmate {}"
		val expected =
			"""
				TypeDefinition [ class Identifier { Dog } UnionType { SimpleType { Identifier { Animal } } & SimpleType { Identifier { Soulmate } } } ] { TypeBody {
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testInnerType() {
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
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testTraitDeclaration() {
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
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testGenericTraitDeclaration() {
		val sourceCode =
			"""
				trait Comparable {
					containing Target
				}
            """.trimIndent()
		val expected =
			"""
				TypeDefinition [ trait Identifier { Comparable } ] { TypeBody {
					GenericsDeclaration {
						GenericsListElement { Identifier { Target } }
					}
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testEnumDeclaration() {
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
						Identifier { Pending }
						Identifier { Cancelled }
						Identifier { Delivered }
					}
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testTypeAlias() {
		val sourceCode =
			"""
				alias EventHandler = (Event) =>;
            """.trimIndent()
		val expected =
			"""
				TypeAlias [ Identifier { EventHandler } ] { LambdaFunctionType { LambdaParameterList {
					SimpleType { Identifier { Event } }
				} } }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}