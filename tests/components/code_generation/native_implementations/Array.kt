package components.code_generation.native_implementations

import components.code_generation.llvm.Llvm
import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Array {

	@Test
	fun `plural type initializer sets size`() {
		val sourceCode = """
			SimplestApp object {
				to getOne(): Int {
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
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne", mapOf(
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME,
			SpecialType.STRING to TestUtil.TEST_FILE_NAME
		))
		assertEquals(3, Llvm.castToSignedInteger(result))
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
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME,
			SpecialType.STRING to TestUtil.TEST_FILE_NAME
		))
		assertEquals(3, Llvm.castToSignedInteger(result))
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
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME,
			SpecialType.STRING to TestUtil.TEST_FILE_NAME
		))
		assertEquals(5, Llvm.castToSignedInteger(result))
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
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME,
			SpecialType.STRING to TestUtil.TEST_FILE_NAME
		))
		assertEquals(7, Llvm.castToSignedInteger(result))
	}
}
