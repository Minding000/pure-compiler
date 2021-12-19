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
						Function [Identifier { getSound }(
							TypedIdentifier { Identifier { loudness } : Type { Identifier { Int } } }
						): void] { StatementBlock {
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
						Function [Identifier { getSound }(
							TypedIdentifier { Identifier { loudness } : Type { Identifier { Int } } }
						): void] { StatementBlock {
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
}