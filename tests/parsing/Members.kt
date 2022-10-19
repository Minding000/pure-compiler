package parsing

import util.TestUtil
import org.junit.jupiter.api.Test

internal class Members {

	@Test
	fun `parses constant members`() {
		val sourceCode = """
			class Human {
				const: Int {
					EYE_COUNT = 2
					ARM_COUNT = 2
					LEG_COUNT = 2
				}
			}
		""".trimIndent()
		val expected =
			"""
				TypeDefinition [ class Identifier { Human } ] { TypeBody {
					VariableSection [ const: ObjectType { Identifier { Int } } ] {
						VariableDeclaration { Identifier { EYE_COUNT } = NumberLiteral { 2 } }
						VariableDeclaration { Identifier { ARM_COUNT } = NumberLiteral { 2 } }
						VariableDeclaration { Identifier { LEG_COUNT } = NumberLiteral { 2 } }
					}
				} }
            """.trimIndent()
		TestUtil.assertSameSyntaxTree(expected, sourceCode)
	}

	@Test
	fun `parses computed members`() {
		val sourceCode = """
			class Rectangle {
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
				TypeDefinition [ class Identifier { Rectangle } ] { TypeBody {
					GenericsDeclaration {
						Parameter { Identifier { Unit }: ObjectType { Identifier { Number } } }
					}
					VariableSection [ var: ObjectType { Identifier { Unit } } = NumberLiteral { 0 } ] {
						VariableDeclaration { Identifier { left } }
						VariableDeclaration { Identifier { right } }
						VariableDeclaration { Identifier { top } }
						VariableDeclaration { Identifier { bottom } }
					}
					VariableSection [ val: ObjectType { Identifier { Unit } } ] {
						ComputedProperty {
							Identifier { width }
							gets BinaryOperator {
								Identifier { right } - Identifier { left }
							}
						}
						ComputedProperty {
							Identifier { height }
							gets BinaryOperator {
								Identifier { bottom } - Identifier { top }
							}
						}
						ComputedProperty {
							Identifier { centerX }
							gets BinaryOperator {
								Identifier { left } + BinaryOperator {
									Identifier { width } / NumberLiteral { 2 }
								}
							}
						}
						ComputedProperty {
							Identifier { centerY }
							gets BinaryOperator {
								Identifier { top } + BinaryOperator {
									Identifier { height } / NumberLiteral { 2 }
								}
							}
						}
					}
					VariableSection [ val ] {
						ComputedProperty {
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
}
