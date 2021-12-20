package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class FunctionDefinitionTest {

	@Test
	fun testFunctionDeclaration() {
		val sourceCode = "class Animal { fun getSound(loudness: Int) {  } }"
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Animal }] { TypeBody {
						Function [Identifier { getSound } ParameterList {
							TypedIdentifier { Identifier { loudness } : Type { Identifier { Int } } }
						}: void] { StatementBlock {
						} }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testFunctionBody() {
		val sourceCode = "class Animal { fun getSound(loudness: Int) { var energy = loudness * 2 } }"
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Animal }] { TypeBody {
						Function [Identifier { getSound } ParameterList {
							TypedIdentifier { Identifier { loudness } : Type { Identifier { Int } } }
						}: void] { StatementBlock {
							VariableDeclaration {
								Assignment {
									Identifier { energy } = BinaryOperator {
										Identifier { loudness } * NumberLiteral { 2 }
									}
								}
							}
						} }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testConstructorDeclaration() {
		val sourceCode = """
			class Animal {
				var canSwim: Bool
				
				init(name: String, canSwim) {
					echo "Creating", name
				}
			}""".trimIndent()
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Animal }] { TypeBody {
						PropertyDeclaration [] {
							TypedIdentifier { Identifier { canSwim } : Type { Identifier { Bool } } }
						}
						Initializer [
							TypedIdentifier { Identifier { name } : Type { Identifier { String } } }
							Identifier { canSwim }
						] { StatementBlock {
							Print {
								StringLiteral { "Creating" }
								Identifier { name }
							}
						} }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testConstructorShorthand() {
		val sourceCode = """
			class Animal {
				var canSwim: Bool
				
				init(canSwim)
			}""".trimIndent()
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Animal }] { TypeBody {
						PropertyDeclaration [] {
							TypedIdentifier { Identifier { canSwim } : Type { Identifier { Bool } } }
						}
						Initializer [
							Identifier { canSwim }
						] {  }
					} }
				}
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
				Program {
					TypeDefinition [TypeType { class } Identifier { Vector }] { TypeBody {
						PropertyDeclaration [] {
							TypedIdentifier { Identifier { x } : Type { Identifier { Int } } }
							TypedIdentifier { Identifier { y } : Type { Identifier { Int } } }
						}
						OperatorDefinition [Operator { += } ParameterList {
							TypedIdentifier { Identifier { right } : Type { Identifier { Vector } } }
						}: void] { StatementBlock {
							BinaryModification {
								Identifier { x } += ReferenceChain {
									Identifier { right }
									Identifier { x }
								}
							}
							BinaryModification {
								Identifier { y } += ReferenceChain {
									Identifier { right }
									Identifier { y }
								}
							}
						} }
						OperatorDefinition [Operator { == } ParameterList {
							TypedIdentifier { Identifier { right } : Type { Identifier { Vector } } }
						}: void] { StatementBlock {
							Return { BinaryOperator {
								BinaryOperator {
									ReferenceChain {
										Identifier { right }
										Identifier { x }
									} == Identifier { x }
								} & BinaryOperator {
									ReferenceChain {
										Identifier { right }
										Identifier { y }
									} == Identifier { y }
								}
							} }
						} }
					} }
				}
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
				Program {
					TypeDefinition [TypeType { class } Identifier { BookSelf }] { TypeBody {
						OperatorDefinition [IndexOperator {
							TypedIdentifier { Identifier { index } : Type { Identifier { Int } } }
						} ParameterList {
							TypedIdentifier { Identifier { value } : Type { Identifier { Book } } }
						}: void] { StatementBlock {
							Print {
								StringLiteral { "Adding book" }
								Identifier { index }
								Identifier { value }
							}
						} }
						OperatorDefinition [IndexOperator {
							TypedIdentifier { Identifier { index } : Type { Identifier { Int } } }
						}: Type { Identifier { Book } }] { StatementBlock {
							Print {
								StringLiteral { "Book requested" }
								Identifier { index }
							}
						} }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}