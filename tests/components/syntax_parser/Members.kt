package components.syntax_parser

import messages.Message
import util.TestUtil
import org.junit.jupiter.api.Test

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
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses computed members`() { //TODO also test setters
		val sourceCode = """
			Rectangle class {
				containing Unit: Number

				var: Unit = 0 {
					left
					right
					top
					bottom
				}
				val: Unit {
					width gets right - left
					height gets bottom - top
					centerX gets left + width / 2
					centerY gets top + height / 2
				}
				val isSquare: Bool
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
					VariableSection [ val: ObjectType { Identifier { Unit } } ] {
						ComputedPropertyDeclaration {
							Identifier { width }
							gets BinaryOperator {
								Identifier { right } - Identifier { left }
							}
						}
						ComputedPropertyDeclaration {
							Identifier { height }
							gets BinaryOperator {
								Identifier { bottom } - Identifier { top }
							}
						}
						ComputedPropertyDeclaration {
							Identifier { centerX }
							gets BinaryOperator {
								Identifier { left } + BinaryOperator {
									Identifier { width } / NumberLiteral { 2 }
								}
							}
						}
						ComputedPropertyDeclaration {
							Identifier { centerY }
							gets BinaryOperator {
								Identifier { top } + BinaryOperator {
									Identifier { height } / NumberLiteral { 2 }
								}
							}
						}
					}
					VariableSection [ val ] {
						ComputedPropertyDeclaration {
							Identifier { isSquare }: ObjectType { Identifier { Bool } }
							gets BinaryOperator {
								Identifier { width } == Identifier { height }
							}
						}
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `initializers can not be declared outside of type definition`() {
		val sourceCode = """
			native init() {}
			""".trimIndent()
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertMessageEmitted(Message.Type.ERROR, "Unexpected INIT")
	}

	@Test
	fun `functions can not be declared outside of type definition`() {
		val sourceCode = """
			native to fillCup() {}
			""".trimIndent()
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertMessageEmitted(Message.Type.ERROR, "Unexpected TO")
	}

	@Test
	fun `operators can not be declared outside of type definition`() {
		val sourceCode = """
			native operator ++() {}
			""".trimIndent()
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertMessageEmitted(Message.Type.ERROR, "Unexpected OPERATOR")
	}

	@Test
	fun `computed properties can not be declared outside of type definition`() {
		val sourceCode = """
			val x gets 3
			""".trimIndent()
		val parseResult = TestUtil.parse(sourceCode)
		parseResult.assertMessageEmitted(Message.Type.ERROR, "Unexpected GETS")
	}
}
