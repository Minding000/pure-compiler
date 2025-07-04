package components.syntax_parser

import org.junit.jupiter.api.Test
import util.TestUtil

internal class TypeDefinitions {

	@Test
	fun `parses class definitions without body`() {
		val sourceCode = "Animal class"
		val expected =
			"""
				TypeDefinition [ Identifier { Animal } class ]
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses class definitions with body`() {
		val sourceCode = "Animal class {}"
		val expected =
			"""
				TypeDefinition [ Identifier { Animal } class ] { TypeBody {
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses object definitions without body`() {
		val sourceCode = "Dog object"
		val expected =
			"""
				TypeDefinition [ Identifier { Dog } object ]
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses object definitions with body`() {
		val sourceCode = "Dog object {}"
		val expected =
			"""
				TypeDefinition [ Identifier { Dog } object ] { TypeBody {
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses enum definitions`() {
		val sourceCode =
			"""
				DeliveryStatus enum {
					instances
						Pending,
						Cancelled,
						Delivered
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
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
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
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses abstract instances`() {
		val sourceCode =
			"""
				abstract Number class {
					abstract instances ZERO
				}
            """.trimIndent()
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { abstract } } ] {
					TypeDefinition [ Identifier { Number } class ] { TypeBody {
						ModifierSection [ ModifierList { Modifier { abstract } } ] {
							InstanceList {
								Instance [ Identifier { ZERO } ] {
								}
							}
						}
					} }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	//TODO change type alias syntax to match other type declaration syntax by moving the 'alias' keyword behind the identifier
	//TODO support explicit parent type to be declared
	@Test
	fun `parses type aliases`() {
		val sourceCode =
			"""
				alias EventHandler = (Event) =>|
            """.trimIndent()
		val expected =
			"""
				TypeAlias [ Identifier { EventHandler } = FunctionType { ParameterTypeList {
					ObjectType { Identifier { Event } }
				} } ]
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses type aliases with instances`() {
		val sourceCode =
			"""
				alias ExitCode = Int {
					instances SUCCESS(0), ERROR(1)
				}
            """.trimIndent()
		val expected =
			"""
				TypeAlias [ Identifier { ExitCode } = ObjectType { Identifier { Int } } ] {
					InstanceList {
						Instance [ Identifier { SUCCESS } ] {
							NumberLiteral { 0 }
						}
						Instance [ Identifier { ERROR } ] {
							NumberLiteral { 1 }
						}
					}
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses type definitions with inheritance`() {
		val sourceCode = "Dog class: Animal & Soulmate"
		val expected =
			"""
				TypeDefinition [ Identifier { Dog } class: UnionType { ObjectType { Identifier { Animal } } & ObjectType { Identifier { Soulmate } } } ]
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
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
					TypeDefinition [ Identifier { Index } class ]
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses explicit parent types`() {
		val sourceCode = "ArmRest class in Couch"
		val expected =
			"""
				TypeDefinition [ Identifier { ArmRest } class in ObjectType { Identifier { Couch } } ]
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses abstract modifiers`() {
		val sourceCode = "abstract Goldfish class"
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { abstract } } ] {
					TypeDefinition [ Identifier { Goldfish } class ]
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses bound modifiers`() {
		val sourceCode = "bound ArmRest class"
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { bound } } ] {
					TypeDefinition [ Identifier { ArmRest } class ]
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses copied modifiers`() {
		val sourceCode = "copied Distance class"
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { copied } } ] {
					TypeDefinition [ Identifier { Distance } class ]
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses converting modifiers`() {
		val sourceCode = """
			Submarine class {
				converting init
			}
		""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Submarine } class ] { TypeBody {
					ModifierSection [ ModifierList { Modifier { converting } } ] {
						Initializer
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses native modifiers`() {
		val sourceCode = "native Goldfish class"
		val expected =
			"""
				ModifierSection [ ModifierList { Modifier { native } } ] {
					TypeDefinition [ Identifier { Goldfish } class ]
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}
}
