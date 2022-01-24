package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class TypeDefinitionTest {

	@Test
	fun testClassDeclaration() {
		val sourceCode = "class Animal {}"
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Animal }] { TypeBody {
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testObjectDeclaration() {
		val sourceCode = "object Dog {}"
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { object } Identifier { Dog }] { TypeBody {
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testNativeDeclaration() {
		val sourceCode = "native class Goldfish {}"
		val expected =
			"""
				Program {
					TypeDefinition [ModifierList { Modifier { native } } TypeType { class } Identifier { Goldfish }] { TypeBody {
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testInheritance() {
		val sourceCode = "class Dog: Animal {}"
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Dog } InheritanceList {
						Type { Identifier { Animal } }
					}] { TypeBody {
					} }
				}
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
				Program {
					TypeDefinition [TypeType { class } Identifier { String }] { TypeBody {
						TypeDefinition [TypeType { class } Identifier { Index }] { TypeBody {
						} }
					} }
				}
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
				Program {
					TypeDefinition [TypeType { trait } Identifier { Printable }] { TypeBody {
					} }
				}
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
				Program {
					TypeDefinition [TypeType { trait } Identifier { Comparable }] { TypeBody {
						GenericsDeclaration {
							Identifier { Target }
						}
					} }
				}
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
				Program {
					TypeDefinition [TypeType { enum } Identifier { DeliveryStatus }] { TypeBody {
						InstanceList {
							Identifier { Pending }
							Identifier { Cancelled }
							Identifier { Delivered }
						}
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}