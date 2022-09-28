package parsing

import util.TestUtil
import org.junit.jupiter.api.Test

internal class FunctionDefinitions {

	@Test
	fun `parses function definitions`() {
		val sourceCode = """
			trait Animal {
				it canEat(food: Food): Bool
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ trait Identifier { Animal } ] { TypeBody {
					FunctionSection [ it ] {
						Function [ Identifier { canEat } ParameterList {
							Parameter { Identifier { food }: ObjectType { Identifier { Food } } }
						}: ObjectType { Identifier { Bool } } ] {  }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses function definitions with complex return type`() {
		val sourceCode = """
			class Box {
				to getContent(): Box? {}
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Box } ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { getContent } ParameterList {
						}: QuantifiedType { ObjectType { Identifier { Box } }? } ] { StatementSection { StatementBlock {
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses function definitions with body`() {
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
							Parameter { Identifier { loudness }: ObjectType { Identifier { Int } } }
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
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses initializer definitions`() {
		val sourceCode = """
			class Animal {
				var canSwim: Bool
				
				init(canSwim)
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Animal } ] { TypeBody {
					VariableSection [ var ] {
						VariableDeclaration { Identifier { canSwim }: ObjectType { Identifier { Bool } } }
					}
					Initializer [ ParameterList {
						Parameter { Identifier { canSwim } }
					} ] {  }
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses initializer definitions with body`() {
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
						VariableDeclaration { Identifier { canSwim }: ObjectType { Identifier { Bool } } }
					}
					Initializer [ ParameterList {
						Parameter { Identifier { name }: ObjectType { Identifier { String } } }
						Parameter { Identifier { canSwim } }
					} ] { StatementSection { StatementBlock {
						Print {
							StringLiteral { "Creating" }
							Identifier { name }
						}
					} } }
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses deinitializer definitions`() {
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
						VariableDeclaration { Identifier { name }: ObjectType { Identifier { String } } }
					}
					Deinitializer { StatementSection { StatementBlock {
						Print {
							StringLiteral { "Animal '${'$'}name' has been deallocated." }
						}
					} } }
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses operator definitions`() {
		val sourceCode = """
			class Vector {
				var: Int {
					x
					y
				}
				
				operator {
					+=(right: Vector) {
						x += right.x
						y += right.y
					}
					
					==(right: Vector) {
						return right.x == x & right.y == y
					}
				}
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Vector } ] { TypeBody {
					VariableSection [ var: ObjectType { Identifier { Int } } ] {
						VariableDeclaration { Identifier { x } }
						VariableDeclaration { Identifier { y } }
					}
					OperatorSection {
						OperatorDefinition [ Operator { += } ParameterList {
							Parameter { Identifier { right }: ObjectType { Identifier { Vector } } }
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
							Parameter { Identifier { right }: ObjectType { Identifier { Vector } } }
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
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses index operator definitions`() {
		val sourceCode = """
			class BookSelf {
				
				operator {
					[index: Int](value: Book) {
						echo "Adding book", index, value
					}
					
					[index: Int]: Book {
						echo "Book requested", index
					}
				}
			}""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { BookSelf } ] { TypeBody {
					OperatorSection {
						OperatorDefinition [ IndexOperator {
							Parameter { Identifier { index }: ObjectType { Identifier { Int } } }
						} ParameterList {
							Parameter { Identifier { value }: ObjectType { Identifier { Book } } }
						}: void ] { StatementSection { StatementBlock {
							Print {
								StringLiteral { "Adding book" }
								Identifier { index }
								Identifier { value }
							}
						} } }
						OperatorDefinition [ IndexOperator {
							Parameter { Identifier { index }: ObjectType { Identifier { Int } } }
						}: ObjectType { Identifier { Book } } ] { StatementSection { StatementBlock {
							Print {
								StringLiteral { "Book requested" }
								Identifier { index }
							}
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses dynamically sized parameters`() {
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
							Parameter [ ModifierList { Modifier { ... } } ] { Identifier { sounds }: QuantifiedType { ...ObjectType { Identifier { Sound } } } }
						}: void ] { StatementSection { StatementBlock {
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses lambda function definitions`() {
		val sourceCode = """
			val condition = (a: Int, b: Int) => { return a < b }
			""".trimIndent()
		val expected =
			"""
				VariableSection [ val ] {
					VariableDeclaration { Identifier { condition } = LambdaFunctionDefinition [ ParameterList {
						Parameter { Identifier { a }: ObjectType { Identifier { Int } } }
						Parameter { Identifier { b }: ObjectType { Identifier { Int } } }
					} ] { StatementSection { StatementBlock {
						Return { BinaryOperator {
							Identifier { a } < Identifier { b }
						} }
					} } } }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}