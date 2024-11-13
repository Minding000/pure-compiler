package components.syntax_parser

import org.junit.jupiter.api.Test
import util.TestUtil

internal class FunctionDefinitionDefinitions {

	@Test
	fun `parses function definitions`() {
		val sourceCode = """
				Animal class {
					it canEat(food: Food): Bool
				}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Animal } class ] { TypeBody {
					FunctionSection [ it ] {
						Function [ Identifier { canEat } ParameterList {
							Parameter { Identifier { food }: ObjectType { Identifier { Food } } }
						}: ObjectType { Identifier { Bool } } ]
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses function definitions with complex return type`() {
		val sourceCode = """
				Box class {
					to getContent(): Box? {}
				}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Box } class ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { getContent } ParameterList {
						}: QuantifiedType { ObjectType { Identifier { Box } }? } ] { StatementSection { StatementBlock {
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses function definitions with body`() {
		val sourceCode = """
				Animal class {
					to getSound(loudness: Int) {
						var energy = loudness * 2
					}
				}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Animal } class ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { getSound } ParameterList {
							Parameter { Identifier { loudness }: ObjectType { Identifier { Int } } }
						}: void ] { StatementSection { StatementBlock {
							VariableSection [ var ] {
								LocalVariableDeclaration { Identifier { energy } = BinaryOperator {
									Identifier { loudness } Operator { * } NumberLiteral { 2 }
								} }
							}
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses initializer definitions inside of type definitions`() {
		val sourceCode = """
				Animal class {
					var canSwim: Bool

					init(canSwim)
				}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Animal } class ] { TypeBody {
					VariableSection [ var ] {
						PropertyDeclaration { Identifier { canSwim }: ObjectType { Identifier { Bool } } }
					}
					Initializer [ ParameterList {
						Parameter { Identifier { canSwim } }
					} ]
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses initializer definitions without parameter list`() {
		val sourceCode = """
				Chair class {
					init
				}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Chair } class ] { TypeBody {
					Initializer
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses initializer definitions with body`() {
		val sourceCode = """
				Animal class {
					var canSwim: Bool
					init(name: String, canSwim) {}
				}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Animal } class ] { TypeBody {
					VariableSection [ var ] {
						PropertyDeclaration { Identifier { canSwim }: ObjectType { Identifier { Bool } } }
					}
					Initializer [ ParameterList {
						Parameter { Identifier { name }: ObjectType { Identifier { String } } }
						Parameter { Identifier { canSwim } }
					} ] { StatementSection { StatementBlock {
					} } }
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses deinitializer definitions`() {
		val sourceCode = """
				Animal class {
					deinit {}
				}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Animal } class ] { TypeBody {
					Deinitializer { StatementSection { StatementBlock {
					} } }
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses operator definitions`() {
		val sourceCode = """
				Vector class {
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
							return right.x == x and right.y == y
						}
					}
				}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Vector } class ] { TypeBody {
					VariableSection [ var: ObjectType { Identifier { Int } } ] {
						PropertyDeclaration { Identifier { x } }
						PropertyDeclaration { Identifier { y } }
					}
					OperatorSection {
						OperatorDefinition [ Operator { += } ParameterList {
							Parameter { Identifier { right }: ObjectType { Identifier { Vector } } }
						}: void ] { StatementSection { StatementBlock {
							BinaryModification {
								Identifier { x } Operator { += } MemberAccess {
									Identifier { right }.Identifier { x }
								}
							}
							BinaryModification {
								Identifier { y } Operator { += } MemberAccess {
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
									} Operator { == } Identifier { x }
								} Operator { and } BinaryOperator {
									MemberAccess {
										Identifier { right }.Identifier { y }
									} Operator { == } Identifier { y }
								}
							} }
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses index operator definitions`() {
		val sourceCode = """
				BookSelf class {

					operator {
						[index: Int](value: Book) {}

						[index: Int]: Book {}
					}
				}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { BookSelf } class ] { TypeBody {
					OperatorSection {
						OperatorDefinition [ IndexOperator { ParameterList {
							Parameter { Identifier { index }: ObjectType { Identifier { Int } } }
						} } ParameterList {
							Parameter { Identifier { value }: ObjectType { Identifier { Book } } }
						}: void ] { StatementSection { StatementBlock {
						} } }
						OperatorDefinition [ IndexOperator { ParameterList {
							Parameter { Identifier { index }: ObjectType { Identifier { Int } } }
						} }: ObjectType { Identifier { Book } } ] { StatementSection { StatementBlock {
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses dynamically sized parameters`() {
		val sourceCode = """
				Animal class {
					to setSounds(...sounds: ...Sound)
				}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Animal } class ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { setSounds } ParameterList {
							Parameter [ ModifierList { Modifier { ... } } ] { Identifier { sounds }: QuantifiedType { ...ObjectType { Identifier { Sound } } } }
						}: void ]
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses lambda function definitions`() {
		val sourceCode = """
				val condition = (a: Int, b: Int) => { return a < b }
			""".trimIndent()
		val expected =
			"""
				VariableSection [ val ] {
					LocalVariableDeclaration { Identifier { condition } = LambdaFunctionDefinition [ ParameterList {
						Parameter { Identifier { a }: ObjectType { Identifier { Int } } }
						Parameter { Identifier { b }: ObjectType { Identifier { Int } } }
					} ] { StatementSection { StatementBlock {
						Return { BinaryOperator {
							Identifier { a } Operator { < } Identifier { b }
						} }
					} } } }
				}
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}
}
