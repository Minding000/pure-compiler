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
				TypeDefinition [ class Identifier { Animal } ] { TypeBody {
					FunctionSection [ it ] {
						Function [ Identifier { canEat } ParameterList {
							Parameter { Identifier { food }: SimpleType { Identifier { Food } } }
						}: SimpleType { Identifier { Bool } } ] { StatementSection { StatementBlock {
						} } }
					}
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
				TypeDefinition [ class Identifier { Animal } ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { getSound } ParameterList {
							Parameter { Identifier { loudness }: SimpleType { Identifier { Int } } }
						}: void ] { StatementSection { StatementBlock {
							VariableSection [ var ] {
								VariableDeclaration { Identifier { energy } = BinaryOperator {
									Identifier { loudness } * NumberLiteral { 2 }
								} }
							}
						} } }
					}
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
				TypeDefinition [ class Identifier { Animal } ] { TypeBody {
					VariableSection [ var ] {
						VariableDeclaration { Identifier { canSwim }: SimpleType { Identifier { Bool } } }
					}
					Initializer [ ParameterList {
						Parameter { Identifier { name }: SimpleType { Identifier { String } } }
						Parameter { Identifier { canSwim } }
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
				TypeDefinition [ class Identifier { Animal } ] { TypeBody {
					VariableSection [ var ] {
						VariableDeclaration { Identifier { canSwim }: SimpleType { Identifier { Bool } } }
					}
					Initializer [ ParameterList {
						Parameter { Identifier { canSwim } }
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
				TypeDefinition [ class Identifier { Animal } ] { TypeBody {
					VariableSection [ var ] {
						VariableDeclaration { Identifier { name }: SimpleType { Identifier { String } } }
					}
					Deinitializer { StatementSection { StatementBlock {
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
				var: Int {
					x
					y
				}
				
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
				TypeDefinition [ class Identifier { Vector } ] { TypeBody {
					VariableSection [ var: SimpleType { Identifier { Int } } ] {
						VariableDeclaration { Identifier { x } }
						VariableDeclaration { Identifier { y } }
					}
					OperatorDefinition [ Operator { += } ParameterList {
						Parameter { Identifier { right }: SimpleType { Identifier { Vector } } }
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
						Parameter { Identifier { right }: SimpleType { Identifier { Vector } } }
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
				TypeDefinition [ class Identifier { BookSelf } ] { TypeBody {
					OperatorDefinition [ IndexOperator {
						Parameter { Identifier { index }: SimpleType { Identifier { Int } } }
					} ParameterList {
						Parameter { Identifier { value }: SimpleType { Identifier { Book } } }
					}: void ] { StatementSection { StatementBlock {
						Print {
							StringLiteral { "Adding book" }
							Identifier { index }
							Identifier { value }
						}
					} } }
					OperatorDefinition [ IndexOperator {
						Parameter { Identifier { index }: SimpleType { Identifier { Int } } }
					}: SimpleType { Identifier { Book } } ] { StatementSection { StatementBlock {
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
				TypeDefinition [ class Identifier { Animal } ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { getSound } ParameterList {
							Parameter { Identifier { loudness }: SimpleType { Identifier { Int } } }
						}: void ] { StatementSection { StatementBlock {
						} } }
					}
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
				TypeDefinition [ class Identifier { Animal } ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { setSounds } ParameterList {
							Parameter [ ModifierList { Modifier { ... } } ] { Identifier { sounds }: QuantifiedType { ...SimpleType { Identifier { Sound } } } }
						}: void ] { StatementSection { StatementBlock {
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testLambdaFunctionDefinition() {
		val sourceCode = """
			val condition = (a: Int, b: Int) => { return a < b }
			""".trimIndent()
		val expected =
			"""
				VariableSection [ val ] {
					VariableDeclaration { Identifier { condition } = LambdaFunctionDefinition [ ParameterList {
						Parameter { Identifier { a }: SimpleType { Identifier { Int } } }
						Parameter { Identifier { b }: SimpleType { Identifier { Int } } }
					} ] { StatementSection { StatementBlock {
						Return { BinaryOperator {
							Identifier { a } < Identifier { b }
						} }
					} } } }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}