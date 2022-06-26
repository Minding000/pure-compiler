package linter

import TestUtil
import linter.messages.Message
import org.junit.jupiter.api.Test

internal class ReferenceResolutionTest {

	@Test
	fun testVariableAccess() {
		val sourceCode =
			"""
				val numberOfCats = 2
				numberOfCats
				numberOfDogs
            """.trimIndent()
		TestUtil.assertLinterMessageNotEmitted(Message.Type.ERROR, "Value 'numberOfCats' hasn't been declared yet", sourceCode)
		TestUtil.assertLinterMessageEmitted(Message.Type.ERROR, "Value 'numberOfDogs' hasn't been declared yet", sourceCode)
	}

	@Test
	fun testMemberAccess() {
		val sourceCode =
			"""
				object House {
					val livingAreaInSquareMeters = 120
				}
				House.livingAreaInSquareMeters
				House.totalAreaInSquareMeters
            """.trimIndent()
		TestUtil.assertLinterMessageNotEmitted(Message.Type.ERROR, "Value 'livingAreaInSquareMeters' hasn't been declared yet", sourceCode)
		TestUtil.assertLinterMessageEmitted(Message.Type.ERROR, "Value 'totalAreaInSquareMeters' hasn't been declared yet", sourceCode)
	}

	@Test
	fun testParameterAccess() {
		val sourceCode =
			"""
				object House {
					to openDoor(speed: Int) {
						speed
						distance
					}
				}
            """.trimIndent()
		TestUtil.assertLinterMessageNotEmitted(Message.Type.ERROR, "Value 'speed' hasn't been declared yet", sourceCode)
		TestUtil.assertLinterMessageEmitted(Message.Type.ERROR, "Value 'distance' hasn't been declared yet", sourceCode)
	}

	@Test
	fun testInitializerAccess() {
		val sourceCode =
			"""
				native class Int {}
				class Window {
					init(width: Int, height: Int) {}
				}
				Window(2, 2)
				Window(2)
            """.trimIndent()
		TestUtil.assertLinterMessageNotEmitted(Message.Type.ERROR, "Initializer 'Window(Int, Int)' hasn't been declared yet", sourceCode)
		TestUtil.assertLinterMessageEmitted(Message.Type.ERROR, "Initializer 'Window(Int)' hasn't been declared yet", sourceCode)
	}

	@Test
	fun testFunctionAccess() {
		val sourceCode =
			"""
				object Door {
					to open() {}
				}
				Door.open()
				Door.close()
            """.trimIndent()
		TestUtil.assertLinterMessageNotEmitted(Message.Type.ERROR, "Function 'open()' hasn't been declared yet", sourceCode)
		TestUtil.assertLinterMessageEmitted(Message.Type.ERROR, "Function 'close()' hasn't been declared yet", sourceCode)
	}

	@Test
	fun testOperatorAccess() {
		val sourceCode =
			"""
				class Matrix {
					init
					operator +(other: Matrix): Matrix {}
				}
				val {
					a = Matrix()
					b = Matrix()
				}
				var c = a + b
				var d = a - b
            """.trimIndent()
		TestUtil.assertLinterMessageNotEmitted(Message.Type.ERROR, "Operator '+(Matrix)' hasn't been declared yet", sourceCode)
		TestUtil.assertLinterMessageEmitted(Message.Type.ERROR, "Operator '-(Matrix)' hasn't been declared yet", sourceCode)
	}
}