package components.syntax_parser

import logger.Severity
import logger.issues.parsing.UnexpectedWord
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Members {

	@Test
	fun `parses constant members`() {
		val sourceCode = """
			Human class {
				const: Int {
					EYE_COUNT = 2
					ARM_COUNT = 2
					LEG_COUNT = 2
				}
			}
		""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Human } class ] { TypeBody {
					VariableSection [ const: ObjectType { Identifier { Int } } ] {
						PropertyDeclaration { Identifier { EYE_COUNT } = NumberLiteral { 2 } }
						PropertyDeclaration { Identifier { ARM_COUNT } = NumberLiteral { 2 } }
						PropertyDeclaration { Identifier { LEG_COUNT } = NumberLiteral { 2 } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `parses computed properties`() {
		val sourceCode = """
			Rectangle class {
				containing Unit: Number

				var: Unit = 0 {
					left
					right
					top
					bottom
				}
				computed: Unit {
					width
						gets right - left
						sets right = left + width
					height
						gets bottom - top
						sets bottom = top + height
					centerX
						gets {
							return left + width / 2
						}
						sets {
							val halfWidth = width / 2
							left = centerX - halfWidth
							right = centerX + halfWidth
						}
					centerY
						gets {
							return top + height / 2
						}
						sets {
							val halfHeight = height / 2
							top = centerY - halfHeight
							bottom = centerY + halfHeight
						}
				}
				computed isSquare: Bool
					gets width == height
			}
		""".trimIndent()
		val expected =
			"""
				TypeDefinition [ Identifier { Rectangle } class ] { TypeBody {
					GenericsDeclaration {
						Parameter { Identifier { Unit }: ObjectType { Identifier { Number } } }
					}
					VariableSection [ var: ObjectType { Identifier { Unit } } = NumberLiteral { 0 } ] {
						PropertyDeclaration { Identifier { left } }
						PropertyDeclaration { Identifier { right } }
						PropertyDeclaration { Identifier { top } }
						PropertyDeclaration { Identifier { bottom } }
					}
					ComputedPropertySection [ ObjectType { Identifier { Unit } } ] {
						ComputedPropertyDeclaration {
							Identifier { width }
							gets BinaryOperator {
								Identifier { right } Operator { - } Identifier { left }
							}
							sets Assignment {
								Identifier { right }
								= BinaryOperator {
									Identifier { left } Operator { + } Identifier { width }
								}
							}
						}
						ComputedPropertyDeclaration {
							Identifier { height }
							gets BinaryOperator {
								Identifier { bottom } Operator { - } Identifier { top }
							}
							sets Assignment {
								Identifier { bottom }
								= BinaryOperator {
									Identifier { top } Operator { + } Identifier { height }
								}
							}
						}
						ComputedPropertyDeclaration {
							Identifier { centerX }
							gets StatementSection { StatementBlock {
								Return { BinaryOperator {
									Identifier { left } Operator { + } BinaryOperator {
										Identifier { width } Operator { / } NumberLiteral { 2 }
									}
								} }
							} }
							sets StatementSection { StatementBlock {
								VariableSection [ val ] {
									LocalVariableDeclaration { Identifier { halfWidth } = BinaryOperator {
										Identifier { width } Operator { / } NumberLiteral { 2 }
									} }
								}
								Assignment {
									Identifier { left }
									= BinaryOperator {
										Identifier { centerX } Operator { - } Identifier { halfWidth }
									}
								}
								Assignment {
									Identifier { right }
									= BinaryOperator {
										Identifier { centerX } Operator { + } Identifier { halfWidth }
									}
								}
							} }
						}
						ComputedPropertyDeclaration {
							Identifier { centerY }
							gets StatementSection { StatementBlock {
								Return { BinaryOperator {
									Identifier { top } Operator { + } BinaryOperator {
										Identifier { height } Operator { / } NumberLiteral { 2 }
									}
								} }
							} }
							sets StatementSection { StatementBlock {
								VariableSection [ val ] {
									LocalVariableDeclaration { Identifier { halfHeight } = BinaryOperator {
										Identifier { height } Operator { / } NumberLiteral { 2 }
									} }
								}
								Assignment {
									Identifier { top }
									= BinaryOperator {
										Identifier { centerY } Operator { - } Identifier { halfHeight }
									}
								}
								Assignment {
									Identifier { bottom }
									= BinaryOperator {
										Identifier { centerY } Operator { + } Identifier { halfHeight }
									}
								}
							} }
						}
					}
					ComputedPropertySection [] {
						ComputedPropertyDeclaration {
							Identifier { isSquare }: ObjectType { Identifier { Bool } }
							gets BinaryOperator {
								Identifier { width } Operator { == } Identifier { height }
							}
						}
					}
				} }
            """.trimIndent()
		TestUtil.assertSyntaxTreeEquals(expected, sourceCode)
	}

	@Test
	fun `initializers can not be declared outside of type definition`() {
		val sourceCode = """
			native init() {}
			""".trimIndent()
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertIssueDetected<UnexpectedWord>("""
			Unexpected INITIALIZER in Test.Test:1:7: 'init'.
			native init() {}
			       ^^^^
			Expected declaration instead.
		""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `functions can not be declared outside of type definition`() {
		val sourceCode = """
			native to fillCup() {}
			""".trimIndent()
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertIssueDetected<UnexpectedWord>("""
			Unexpected TO in Test.Test:1:7: 'to'.
			native to fillCup() {}
			       ^^
			Expected declaration instead.
		""".trimIndent())
	}

	@Test
	fun `operators can not be declared outside of type definition`() {
		val sourceCode = """
			native operator ++() {}
			""".trimIndent()
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertIssueDetected<UnexpectedWord>("""
			Unexpected OPERATOR in Test.Test:1:7: 'operator'.
			native operator ++() {}
			       ^^^^^^^^
			Expected declaration instead.
		""".trimIndent())
	}

	@Test
	fun `computed properties can not be declared outside of type definition`() {
		val sourceCode = """
			computed x gets 3
			""".trimIndent()
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertIssueDetected<UnexpectedWord>("""
			Unexpected COMPUTED in Test.Test:1:0: 'computed'.
			computed x gets 3
			^^^^^^^^
			Expected atom instead.
		""".trimIndent())
	}
}
