package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class FunctionDefinitionTest {

	@Test
	fun testFunctionDeclaration() {
		val sourceCode = """
			class Animal {
				it canEat(food: Food): Bool {
				}
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ TypeType { class } Identifier { Animal } ] { TypeBody {
					Function [ Identifier { canEat } ParameterList {
						Parameter [] { TypedIdentifier { Identifier { food } : Type { SimpleType { Identifier { Food } } } } }
					}: Type { SimpleType { Identifier { Bool } } } ] { StatementSection { StatementBlock {
					} } }
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testFunctionBody() {
		val sourceCode = """
			class Animal {
				to getSound(loudness: Int) {
					var energy = loudness * 2
				}
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ TypeType { class } Identifier { Animal } ] { TypeBody {
					Function [ Identifier { getSound } ParameterList {
						Parameter [] { TypedIdentifier { Identifier { loudness } : Type { SimpleType { Identifier { Int } } } } }
					}: void ] { StatementSection { StatementBlock {
						VariableDeclaration [ var ] {
							Assignment {
								Identifier { energy }
								= BinaryOperator {
									Identifier { loudness } * NumberLiteral { 2 }
								}
							}
						}
					} } }
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testInitializerDeclaration() {
		val sourceCode = """
			class Animal {
				var canSwim: Bool
				
				init(name: String, canSwim) {
					echo "Creating", name
				}
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ TypeType { class } Identifier { Animal } ] { TypeBody {
					PropertyDeclaration [ var ] {
						TypedIdentifier { Identifier { canSwim } : Type { SimpleType { Identifier { Bool } } } }
					}
					Initializer [ ParameterList {
						Parameter [] { TypedIdentifier { Identifier { name } : Type { SimpleType { Identifier { String } } } } }
						Parameter [] { Identifier { canSwim } }
					} ] { StatementSection { StatementBlock {
						Print {
							StringLiteral { "Creating" }
							Identifier { name }
						}
					} } }
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testInitializerShorthand() {
		val sourceCode = """
			class Animal {
				var canSwim: Bool
				
				init(canSwim)
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ TypeType { class } Identifier { Animal } ] { TypeBody {
					PropertyDeclaration [ var ] {
						TypedIdentifier { Identifier { canSwim } : Type { SimpleType { Identifier { Bool } } } }
					}
					Initializer [ ParameterList {
						Parameter [] { Identifier { canSwim } }
					} ] {  }
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testDeinitializerShorthand() {
		val sourceCode = """
			class Animal {
				var name: String
				
				deinit {
					echo "Animal '${'$'}name' has been deallocated."
				}
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ TypeType { class } Identifier { Animal } ] { TypeBody {
					PropertyDeclaration [ var ] {
						TypedIdentifier { Identifier { name } : Type { SimpleType { Identifier { String } } } }
					}
					Deinitializer [  ] { StatementSection { StatementBlock {
						Print {
							StringLiteral { "Animal '${'$'}name' has been deallocated." }
						}
					} } }
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testOperatorDefinition() {
		val sourceCode = """
			class Vector {
				var x: Int, y: Int
				
				operator +=(right: Vector) {
					x += right.x
					y += right.y
				}
				
				operator ==(right: Vector) {
					return right.x == x & right.y == y
				}
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ TypeType { class } Identifier { Vector } ] { TypeBody {
					PropertyDeclaration [ var ] {
						TypedIdentifier { Identifier { x } : Type { SimpleType { Identifier { Int } } } }
						TypedIdentifier { Identifier { y } : Type { SimpleType { Identifier { Int } } } }
					}
					OperatorDefinition [ Operator { += } ParameterList {
						Parameter [] { TypedIdentifier { Identifier { right } : Type { SimpleType { Identifier { Vector } } } } }
					}: void ] { StatementSection { StatementBlock {
						BinaryModification {
							Identifier { x } += MemberAccess {
								Identifier { right }.Identifier { x }
							}
						}
						BinaryModification {
							Identifier { y } += MemberAccess {
								Identifier { right }.Identifier { y }
							}
						}
					} } }
					OperatorDefinition [ Operator { == } ParameterList {
						Parameter [] { TypedIdentifier { Identifier { right } : Type { SimpleType { Identifier { Vector } } } } }
					}: void ] { StatementSection { StatementBlock {
						Return { BinaryOperator {
							BinaryOperator {
								MemberAccess {
									Identifier { right }.Identifier { x }
								} == Identifier { x }
							} & BinaryOperator {
								MemberAccess {
									Identifier { right }.Identifier { y }
								} == Identifier { y }
							}
						} }
					} } }
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testIndexOperatorDefinition() {
		val sourceCode = """
			class BookSelf {
				
				operator [index: Int](value: Book) {
					echo "Adding book", index, value
				}
				
				operator [index: Int]: Book {
					echo "Book requested", index
				}
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ TypeType { class } Identifier { BookSelf } ] { TypeBody {
					OperatorDefinition [ IndexOperator {
						TypedIdentifier { Identifier { index } : Type { SimpleType { Identifier { Int } } } }
					} ParameterList {
						Parameter [] { TypedIdentifier { Identifier { value } : Type { SimpleType { Identifier { Book } } } } }
					}: void ] { StatementSection { StatementBlock {
						Print {
							StringLiteral { "Adding book" }
							Identifier { index }
							Identifier { value }
						}
					} } }
					OperatorDefinition [ IndexOperator {
						TypedIdentifier { Identifier { index } : Type { SimpleType { Identifier { Int } } } }
					}: Type { SimpleType { Identifier { Book } } } ] { StatementSection { StatementBlock {
						Print {
							StringLiteral { "Book requested" }
							Identifier { index }
						}
					} } }
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testFunctionTypeDeclaration() {
		val sourceCode = """
			class Animal {
				to getSound(loudness: Int) {
				}
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ TypeType { class } Identifier { Animal } ] { TypeBody {
					Function [ Identifier { getSound } ParameterList {
						Parameter [] { TypedIdentifier { Identifier { loudness } : Type { SimpleType { Identifier { Int } } } } }
					}: void ] { StatementSection { StatementBlock {
					} } }
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testVariableArguments() {
		val sourceCode = """
			class Animal {
				to setSounds(...sounds: ...Sound) {
				}
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ TypeType { class } Identifier { Animal } ] { TypeBody {
					Function [ Identifier { setSounds } ParameterList {
						Parameter [ ModifierList { Modifier { ... } } ] { TypedIdentifier { Identifier { sounds } : Type { ...SimpleType { Identifier { Sound } } } } }
					}: void ] { StatementSection { StatementBlock {
					} } }
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}