package components.syntax_parser

import org.junit.jupiter.api.Test
import util.TestUtil

internal class WhereClauses {

	@Test
	fun `parses where clauses on computed properties`() {
		val sourceCode = """
			Truck class {
				containing Liquid
				computed liquidIgnitionTemperature: Float where Liquid is Ignitable
					gets 0
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Truck } class ] { TypeBody {
					GenericsDeclaration {
						Parameter { Identifier { Liquid } }
					}
					ComputedPropertySection [] {
						ComputedPropertyDeclaration {
							Identifier { liquidIgnitionTemperature }: ObjectType { Identifier { Float } } WhereClause {
								WhereClauseCondition { Identifier { Liquid } is ObjectType { Identifier { Ignitable } } }
							}
							gets NumberLiteral { 0 }
						}
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses where clauses on functions`() {
		val sourceCode = """
			Map class {
				containing Key, Value
				to lowercase() where Key is String and Value is String {}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Map } class ] { TypeBody {
					GenericsDeclaration {
						Parameter { Identifier { Key } }
						Parameter { Identifier { Value } }
					}
					FunctionSection [ to ] {
						Function [ Identifier { lowercase } ParameterList {
						}: void WhereClause {
							WhereClauseCondition { Identifier { Key } is ObjectType { Identifier { String } } }
							WhereClauseCondition { Identifier { Value } is ObjectType { Identifier { String } } }
						} ] { StatementSection { StatementBlock {
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses where clauses on operators`() {
		val sourceCode = """
			Truck class {
				containing Liquid
				operator [componentIndex: Int](): Component where Liquid is Emulsion {}
			}
			""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Truck } class ] { TypeBody {
					GenericsDeclaration {
						Parameter { Identifier { Liquid } }
					}
					OperatorSection {
						OperatorDefinition [ IndexOperator { ParameterList {
							Parameter { Identifier { componentIndex }: ObjectType { Identifier { Int } } }
						} } ParameterList {
						}: ObjectType { Identifier { Component } } WhereClause {
							WhereClauseCondition { Identifier { Liquid } is ObjectType { Identifier { Emulsion } } }
						} ] { StatementSection { StatementBlock {
						} } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}
}
