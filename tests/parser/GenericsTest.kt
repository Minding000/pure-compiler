package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class GenericsTest {

	@Test
	fun testGenericsDeclaration() {
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
				Program {
					TypeDefinition [TypeType { class } Identifier { ShoppingList }] { TypeBody {
						GenericsDeclaration {
							Identifier { Entry }
						}
						Function [Identifier { add } ParameterList {
							Parameter [] { TypedIdentifier { Identifier { entry } : Type { Identifier { Entry } } } }
						}: void] { StatementBlock {
							Print {
								StringLiteral { "Adding entry..." }
							}
						} }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testTypeList() {
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
				Program {
					TypeDefinition [TypeType { class } Identifier { ShoppingList }] { TypeBody {
						GenericsDeclaration {
							Identifier { Entry }
						}
						Function [Identifier { add } ParameterList {
							Parameter [] { TypedIdentifier { Identifier { entry } : Type { Identifier { Entry } } } }
						}: void] { StatementBlock {
							Print {
								StringLiteral { "Adding entry..." }
							}
						} }
					} }
					TypeDefinition [TypeType { class } Identifier { Fruit }] { TypeBody {
						PropertyDeclaration [ var ] {
							TypedIdentifier { Identifier { name } : Type { Identifier { String } } }
						}
					} }
					VariableDeclaration [ var ] {
						Assignment {
							Identifier { fruitList }
							= FunctionCall [Type { TypeList {
								TypeParameter [] { Type { Identifier { Fruit } } }
							} Identifier { ShoppingList } }] {
							}
						}
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testConsumingType() {
		val sourceCode = """
			class Fridge {
				to add(foodList: <Food producing>List) {
				}
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { Fridge }] { TypeBody {
						Function [Identifier { add } ParameterList {
							Parameter [] { TypedIdentifier { Identifier { foodList } : Type { TypeList {
								TypeParameter [ GenericModifier { producing } ] { Type { Identifier { Food } } }
							} Identifier { List } } } }
						}: void] { StatementBlock {
						} }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testProducingType() {
		val sourceCode = """
			class SodaMachine {
				to refill(glass: <Soda consuming>LiquidContainer) {
				}
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { class } Identifier { SodaMachine }] { TypeBody {
						Function [Identifier { refill } ParameterList {
							Parameter [] { TypedIdentifier { Identifier { glass } : Type { TypeList {
								TypeParameter [ GenericModifier { consuming } ] { Type { Identifier { Soda } } }
							} Identifier { LiquidContainer } } } }
						}: void] { StatementBlock {
						} }
					} }
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}