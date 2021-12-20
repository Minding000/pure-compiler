package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class GenericsTest {

	@Test
	fun testGenericsDeclaration() {
		val sourceCode = """
			generic ShoppingList {
				containing Entry
			
				fun add(entry: Entry) {
					echo "Adding entry..."
				}
			}
			""".trimIndent()
		val expected =
			"""
				Program {
					TypeDefinition [TypeType { generic } Identifier { ShoppingList }] { TypeBody {
						GenericsDeclaration {
							Identifier { Entry }
						}
						Function [Identifier { add } ParameterList {
							TypedIdentifier { Identifier { entry } : Type { Identifier { Entry } } }
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
			generic ShoppingList {
				containing Entry
				
				fun add(entry: Entry) {
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
					TypeDefinition [TypeType { generic } Identifier { ShoppingList }] { TypeBody {
						GenericsDeclaration {
							Identifier { Entry }
						}
						Function [Identifier { add } ParameterList {
							TypedIdentifier { Identifier { entry } : Type { Identifier { Entry } } }
						}: void] { StatementBlock {
							Print {
								StringLiteral { "Adding entry..." }
							}
						} }
					} }
					TypeDefinition [TypeType { class } Identifier { Fruit }] { TypeBody {
						PropertyDeclaration [] {
							TypedIdentifier { Identifier { name } : Type { Identifier { String } } }
						}
					} }
					VariableDeclaration {
						Assignment {
							Identifier { fruitList } = FunctionCall [TypeList {
								Type { Identifier { Fruit } }
							} Identifier { ShoppingList }] {
							}
						}
					}
				}
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}