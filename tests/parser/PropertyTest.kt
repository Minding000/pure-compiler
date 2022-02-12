package parser

import TestUtil
import org.junit.jupiter.api.Test

internal class PropertyTest {

	@Test
	fun testComputedProperty() {
		val sourceCode = """
			class Rectangle {
				containing Unit: Number
				
				var left = right = top = bottom = 0 as Unit
				val {
					width: Unit
						get right - left
					height: Unit
						get bottom - top
					centerX: Unit
						get left + width / 2
					centerY: Unit
						get top + height / 2
					isSquare: Bool
						get width == height
				}
			}
		""".trimIndent()
		val expected =
			"""
				TypeDefinition [ TypeType { class } Identifier { Rectangle } ] { TypeBody {
					GenericsDeclaration {
						TypedIdentifier { Identifier { Unit } : Type { SimpleType { Identifier { Number } } } }
					}
					PropertyDeclaration [ var ] {
						Assignment {
							Identifier { left }
							Identifier { right }
							Identifier { top }
							Identifier { bottom }
							= Cast {
								NumberLiteral { 0 } as Type { SimpleType { Identifier { Unit } } }
							}
						}
					}
					PropertyDeclaration [ val ] {
						ComputedProperty {
							TypedIdentifier { Identifier { width } : Type { SimpleType { Identifier { Unit } } } }
							get BinaryOperator {
								Identifier { right } - Identifier { left }
							}
						}
						ComputedProperty {
							TypedIdentifier { Identifier { height } : Type { SimpleType { Identifier { Unit } } } }
							get BinaryOperator {
								Identifier { bottom } - Identifier { top }
							}
						}
						ComputedProperty {
							TypedIdentifier { Identifier { centerX } : Type { SimpleType { Identifier { Unit } } } }
							get BinaryOperator {
								Identifier { left } + BinaryOperator {
									Identifier { width } / NumberLiteral { 2 }
								}
							}
						}
						ComputedProperty {
							TypedIdentifier { Identifier { centerY } : Type { SimpleType { Identifier { Unit } } } }
							get BinaryOperator {
								Identifier { top } + BinaryOperator {
									Identifier { height } / NumberLiteral { 2 }
								}
							}
						}
						ComputedProperty {
							TypedIdentifier { Identifier { isSquare } : Type { SimpleType { Identifier { Bool } } } }
							get BinaryOperator {
								Identifier { width } == Identifier { height }
							}
						}
					}
				} }
            """.trimIndent()
		TestUtil.assertAST(expected, sourceCode)
	}
}