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
				TypeDefinition [ class Identifier { ShoppingList } ] { TypeBody {
					GenericsDeclaration {
						Identifier { Entry }
					}
					FunctionSection [ to ] {
						Function [ Identifier { add } ParameterList {
							Parameter { TypedIdentifier { Identifier { entry }: SimpleType { Identifier { Entry } } } }
						}: void ] { StatementSection { StatementBlock {
							Print {
								StringLiteral { "Adding entry..." }
							}
						} } }
					}
				} }
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
				TypeDefinition [ class Identifier { ShoppingList } ] { TypeBody {
					GenericsDeclaration {
						Identifier { Entry }
					}
					FunctionSection [ to ] {
						Function [ Identifier { add } ParameterList {
							Parameter { TypedIdentifier { Identifier { entry }: SimpleType { Identifier { Entry } } } }
						}: void ] { StatementSection { StatementBlock {
							Print {
								StringLiteral { "Adding entry..." }
							}
						} } }
					}
				} }
				TypeDefinition [ class Identifier { Fruit } ] { TypeBody {
					VariableSection [ var ] {
						VariableDeclaration { Identifier { name }: SimpleType { Identifier { String } } }
					}
				} }
				VariableSection [ var ] {
					VariableDeclaration { Identifier { fruitList } = FunctionCall [ SimpleType { TypeList {
						SimpleType { Identifier { Fruit } }
					} Identifier { ShoppingList } } ] {
					} }
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
				TypeDefinition [ class Identifier { Fridge } ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { add } ParameterList {
							Parameter { TypedIdentifier { Identifier { foodList }: SimpleType { TypeList {
								TypeParameter [ producing ] { SimpleType { Identifier { Food } } }
							} Identifier { List } } } }
						}: void ] { StatementSection { StatementBlock {
						} } }
					}
				} }
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
				TypeDefinition [ class Identifier { SodaMachine } ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { refill } ParameterList {
							Parameter { TypedIdentifier { Identifier { glass }: SimpleType { TypeList {
								TypeParameter [ consuming ] { SimpleType { Identifier { Soda } } }
							} Identifier { LiquidContainer } } } }
						}: void ] { StatementSection { StatementBlock {
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}

	@Test
	fun testGenericFunction() {
		val sourceCode = """
			object Math {
				to max<N: Number>(a: N, b: N): N {
				}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ object Identifier { Math } ] { TypeBody {
					FunctionSection [ to ] {
						Function [ Identifier { max } GenericsList {
							GenericsListElement [ Identifier { N } ] { SimpleType { Identifier { Number } } }
						} ParameterList {
							Parameter { TypedIdentifier { Identifier { a }: SimpleType { Identifier { N } } } }
							Parameter { TypedIdentifier { Identifier { b }: SimpleType { Identifier { N } } } }
						}: SimpleType { Identifier { N } } ] { StatementSection { StatementBlock {
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}