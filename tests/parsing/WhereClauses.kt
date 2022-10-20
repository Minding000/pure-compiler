package parsing

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil

internal class WhereClauses {

	@Disabled
	@Test
	fun `parses where clauses`() {
		val sourceCode = """
			class Truck {
				containing Liquid

				to explode() where Liquid: Ignitable {}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Truck } ] {
					GenericsDeclaration {
						Parameter { Identifier { Liquid } }
					}
					FunctionDefinition {
						WhereClause {
							Condition { Identifier { Liquid }: Identifier { Ignitable } }
						}
					}
				}
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}
}
