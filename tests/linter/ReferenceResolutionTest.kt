package linter

import util.TestUtil
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
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageNotEmitted(Message.Type.ERROR, "Value 'numberOfCats' hasn't been declared yet")
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Value 'numberOfDogs' hasn't been declared yet")
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
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageNotEmitted(Message.Type.ERROR, "Value 'livingAreaInSquareMeters' hasn't been declared yet")
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Value 'totalAreaInSquareMeters' hasn't been declared yet")
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
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageNotEmitted(Message.Type.ERROR, "Value 'speed' hasn't been declared yet")
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Value 'distance' hasn't been declared yet")
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
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageNotEmitted(Message.Type.ERROR, "Initializer 'Window(Int, Int)' hasn't been declared yet")
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Initializer 'Window(Int)' hasn't been declared yet")


		/*lintResult.assertLinterMessageNotEmitted(Message.Type.ERROR, "Value 'Window' hasn't been declared yet")
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Value 'Window' hasn't been declared yet")
		lintResult.assertLinterMessageNotEmitted(Message.Type.ERROR, "'Window' is not callable")
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "'Window' is not callable")
		lintResult.assertLinterMessageNotEmitted(Message.Type.ERROR, "Call to function 'Window()' is ambiguous")
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Call to function 'Window()' is ambiguous")
		lintResult.assertLinterMessageNotEmitted(Message.Type.ERROR, "The provided values don't match any signature of function 'Window'")
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "The provided values don't match any signature of function 'Window'")*/
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
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageNotEmitted(Message.Type.ERROR, "Function 'open()' hasn't been declared yet")
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Function 'close()' hasn't been declared yet")
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
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageNotEmitted(Message.Type.ERROR, "Operator '+(Matrix)' hasn't been declared yet")
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Operator '-(Matrix)' hasn't been declared yet")
	}
}