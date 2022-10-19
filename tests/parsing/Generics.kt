package parsing

import org.junit.jupiter.api.Test
import util.TestUtil

internal class Generics {

	@Test
	fun `parses generics declarations`() {
		val sourceCode = """
			class ShoppingList {
				containing Entry

				to add(entry: Entry) {
					echo "Adding entry..."
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { ShoppingList } ] { TypeBody {
					GenericsDeclaration {
						Parameter { Identifier { Entry } }
					}
					FunctionSection [ to ] {
						Function [ Identifier { add } ParameterList {
							Parameter { Identifier { entry }: ObjectType { Identifier { Entry } } }
						}: void ] { StatementSection { StatementBlock {
							Print {
								StringLiteral { "Adding entry..." }
							}
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses generic function definitions`() {
		val sourceCode = """
			object Math {
				to getGreatest(N: Number; a: N, b: N): N {}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ object Identifier { Math } ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { getGreatest } ParameterList {
							Parameter { Identifier { N }: ObjectType { Identifier { Number } } };
							Parameter { Identifier { a }: ObjectType { Identifier { N } } }
							Parameter { Identifier { b }: ObjectType { Identifier { N } } }
						}: ObjectType { Identifier { N } } ] { StatementSection { StatementBlock {
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses generic operator definitions`() {
		val sourceCode = """
			object Server {
				operator [P: Protocol; protocol: P]: <P>Service {}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ object Identifier { Server } ] { TypeBody {
					OperatorSection {
						OperatorDefinition [ IndexOperator { ParameterList {
							Parameter { Identifier { P }: ObjectType { Identifier { Protocol } } };
							Parameter { Identifier { protocol }: ObjectType { Identifier { P } } }
						} }: ObjectType { TypeList {
							ObjectType { Identifier { P } }
						} Identifier { Service } } ] { StatementSection { StatementBlock {
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses specific initializer calls`() {
		val sourceCode = """
			class ShoppingList {
				containing Entry

				to add(entry: Entry) {
					echo "Adding entry..."
				}
			}
			class Fruit {
				var name: String
			}
			var fruitList = <Fruit>ShoppingList()
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { ShoppingList } ] { TypeBody {
					GenericsDeclaration {
						Parameter { Identifier { Entry } }
					}
					FunctionSection [ to ] {
						Function [ Identifier { add } ParameterList {
							Parameter { Identifier { entry }: ObjectType { Identifier { Entry } } }
						}: void ] { StatementSection { StatementBlock {
							Print {
								StringLiteral { "Adding entry..." }
							}
						} } }
					}
				} }
				TypeDefinition [ class Identifier { Fruit } ] { TypeBody {
					VariableSection [ var ] {
						VariableDeclaration { Identifier { name }: ObjectType { Identifier { String } } }
					}
				} }
				VariableSection [ var ] {
					VariableDeclaration { Identifier { fruitList } = FunctionCall [ TypeSpecification [ TypeList {
						ObjectType { Identifier { Fruit } }
					} ] { Identifier { ShoppingList } } ] {
					} }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses specific function calls`() {
		val sourceCode = """
			Math.getGreatest(Int; 1, 2)
			""".trimIndent()
		val expected =
			"""
				FunctionCall [ MemberAccess {
					Identifier { Math }.Identifier { getGreatest }
				} ] {
					Identifier { Int };
					NumberLiteral { 1 }
					NumberLiteral { 2 }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses producing types`() {
		val sourceCode = """
			class Fridge {
				to add(foodList: <Food producing>List) {}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Fridge } ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { add } ParameterList {
							Parameter { Identifier { foodList }: ObjectType { TypeList {
								TypeParameter [ producing ] { ObjectType { Identifier { Food } } }
							} Identifier { List } } }
						}: void ] { StatementSection { StatementBlock {
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses consuming types`() {
		val sourceCode = """
			class SodaMachine {
				to refill(glass: <Soda consuming>LiquidContainer) {}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { SodaMachine } ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { refill } ParameterList {
							Parameter { Identifier { glass }: ObjectType { TypeList {
								TypeParameter [ consuming ] { ObjectType { Identifier { Soda } } }
							} Identifier { LiquidContainer } } }
						}: void ] { StatementSection { StatementBlock {
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
