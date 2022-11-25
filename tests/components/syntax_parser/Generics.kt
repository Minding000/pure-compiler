package components.syntax_parser

import org.junit.jupiter.api.Test
import util.TestUtil

internal class Generics {

	@Test
	fun `parses generics declarations`() {
		val sourceCode = """
			ShoppingList class {
				containing Entry

				to add(entry: Entry) {
					echo "Adding entry..."
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { ShoppingList } class ] { TypeBody {
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
			Math object {
				to getGreatest(N: Number; a: N, b: N): N {}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Math } object ] { TypeBody {
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
			Server object {
				operator [P: Protocol; protocol: P]: <P>Service {}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Server } object ] { TypeBody {
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
			ShoppingList class {
				containing Entry

				to add(entry: Entry) {
					echo "Adding entry..."
				}
			}
			Fruit class {
				var name: String
			}
			var fruitList = <Fruit>ShoppingList()
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { ShoppingList } class ] { TypeBody {
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
				TypeDefinition [ Identifier { Fruit } class ] { TypeBody {
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
					ObjectType { Identifier { Int } };
					NumberLiteral { 1 }
					NumberLiteral { 2 }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses specific index operator calls`() {
		val sourceCode = """
			shoppingList[Int, ShoppingItem; 1]
			""".trimIndent()
		val expected =
			"""
				Index [ Identifier { shoppingList } ] {
					ObjectType { Identifier { Int } }
					ObjectType { Identifier { ShoppingItem } };
					NumberLiteral { 1 }
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses producing types`() {
		val sourceCode = """
			Fridge class {
				to add(foodList: <Food producing>List) {}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Fridge } class ] { TypeBody {
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
			SodaMachine class {
				to refill(glass: <Soda consuming>LiquidContainer) {}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { SodaMachine } class ] { TypeBody {
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
