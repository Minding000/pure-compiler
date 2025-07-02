package components.code_generation.native_implementations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestApp
import util.TestUtil
import kotlin.test.assertEquals

internal class Array {

	@Test
	fun `plural type initializer sets size`() {
		val sourceCode = """
			SimplestApp object {
				to getThree(): Int {
					val array = Array(3, 2, 1)
					return array.size
				}
			}
			native Array class {
				containing Element
				val size: Int
				native init(...values: ...Element)
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getThree", mapOf(
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME
		))
		assertEquals(3, result)
	}

	@Test
	fun `plural type initializer sets values`() {
		val sourceCode = """
			SimplestApp object {
				to getThree(): Int {
					val array = Array(3, 2, 1)
					return array[0]
				}
			}
			native Array class {
				containing Element
				val size: Int
				native init(...values: ...Element)
				native operator [index: Int]: Element
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getThree", mapOf(
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME
		))
		assertEquals(3, result)
	}

	@Test
	fun `value to be repeated initializer sets size`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					val array = Array(2, 5)
					return array.size
				}
			}
			native Array class {
				containing Element
				val size: Int
				native init(value: Element, size)
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive", mapOf(
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME
		))
		assertEquals(5, result)
	}

	@Test
	fun `value to be repeated initializer sets values`() {
		val sourceCode = """
			SimplestApp object {
				to getSeven(): Int {
					val array = Array(7, 4)
					return array[3]
				}
			}
			native Array class {
				containing Element
				val size: Int
				native init(value: Element, size)
				native operator [index: Int]: Element
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSeven", mapOf(
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME
		))
		assertEquals(7, result)
	}

	@Test
	fun `addition operator sets size`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					val a = Array(7, 2)
					val b = Array(3, 3)
					return (a + b).size
				}
			}
			native Array class {
				containing Element
				val size: Int
				native init(value: Element, size)
				native operator +(right: <Element>Array): <Element>Array
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive", mapOf(
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME
		))
		assertEquals(5, result)
	}

	@Test
	fun `addition operator sets values`() {
		val sourceCode = """
			SimplestApp object {
				to getThree(): Int {
					val a = Array(7, 2)
					val b = Array(3, 3)
					return (a + b)[4]
				}
			}
			native Array class {
				containing Element
				val size: Int
				native init(value: Element, size)
				native operator [index: Int]: Element
				native operator +(right: <Element>Array): <Element>Array
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getThree", mapOf(
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME
		))
		assertEquals(3, result)
	}

	@Test
	fun `get operator gets requested value`() {
		val sourceCode = """
			SimplestApp object {
				to getThree(): Int {
					return Array(1, 2, 3, 4)[2]
				}
			}
			native Array class {
				containing Element
				val size: Int
				native init(...values: ...Element)
				native operator [index: Int]: Element
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getThree", mapOf(
			SpecialType.ARRAY to listOf(TestUtil.TEST_FILE_NAME)
		))
		assertEquals(3, result)
	}

	@Test
	fun `get operator raises if index is negative`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getThree(): Int {
					return Array(1, 2, 3, 4)[-1]
				}
			}
		""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.getThree")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: The index '-1' is outside the arrays bounds (0-3)
			 at Pure:Array:28:[Int]: Element
			 at Test:Test:4:SimplestApp.getThree(): Int
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `get operator raises if index is out of range`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getThree(): Int {
					return Array(1, 2, 3, 4)[4]
				}
			}
		""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.getThree")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: The index '4' is outside the arrays bounds (0-3)
			 at Pure:Array:28:[Int]: Element
			 at Test:Test:4:SimplestApp.getThree(): Int
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `set operator sets specified value`() {
		val sourceCode = """
			SimplestApp object {
				to getFourteen(): Int {
					val bytes = Array(1, 2, 3, 4)
					bytes[3] = 14
					bytes[2] = -1
					return bytes[3]
				}
			}
			native Array class {
				containing Element
				val size: Int
				native init(...values: ...Element)
				native operator [index: Int]: Element
				native operator [index: Int](element: Element)
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFourteen", mapOf(
			SpecialType.ARRAY to listOf(TestUtil.TEST_FILE_NAME)
		))
		assertEquals(14, result)
	}

	@Test
	fun `set operator raises if index is negative`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getFourteen(): Int {
					val bytes = Array(1, 2, 3, 4)
					bytes[-1] = 14
					return bytes[3]
				}
			}
		""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.getFourteen")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: The index '-1' is outside the arrays bounds (0-3)
			 at Pure:Array:29:[Int](Element)
			 at Test:Test:5:SimplestApp.getFourteen(): Int
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}

	@Test
	fun `set operator raises if index is out of range`() {
		val sourceCode = """
			referencing Pure
			SimplestApp object {
				to getFourteen(): Int {
					val bytes = Array(1, 2, 3, 4)
					bytes[4] = 14
					return bytes[3]
				}
			}
		""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.getFourteen")
		app.includeRequiredModules = true
		val expectedOutput = """
			Unhandled error: The index '4' is outside the arrays bounds (0-3)
			 at Pure:Array:29:[Int](Element)
			 at Test:Test:5:SimplestApp.getFourteen(): Int
			""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}
}
