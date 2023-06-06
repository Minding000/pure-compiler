package components.compiler.operators

import components.compiler.targets.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class BinaryFloatOperatorsWithCast {

	@Test
	fun `compiles additions with integer and float operands`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					return 4.0 + 1
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles subtractions with integer and float operands`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					return 7 - 2.0
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles multiplications with integer and float operands`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					return 4 * 1.25
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles divisions with integer and float operands`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Float {
					return 10.0 / 2
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5.0, Llvm.castToFloat(result))
	}

	@Test
	fun `compiles smaller than with integer and float operands`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return 2 < 2.0
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles greater than with integer and float operands`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return 2.0 > 2
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles smaller than or equal to with integer and float operands`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					return 2 <= 2.0
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles greater than or equal to with integer and float operands`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					return 2.0 >= 2
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles equal to with integer and float operands`() {
		val sourceCode = """
			SimplestApp object {
				to getYes(): Bool {
					return 2 == 2.0
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getYes")
		assertEquals(true, Llvm.castToBoolean(result))
	}

	@Test
	fun `compiles not equal to with integer and float operands`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return 2.0 != 2
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, Llvm.castToBoolean(result))
	}
}
