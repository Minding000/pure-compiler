package components.code_generation.native_implementations

import components.code_generation.llvm.Llvm
import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Array {

	@Test
	fun `can be constructed from a plural type`() {
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
	fun `can be constructed from an iterable`() {
		val sourceCode = """
			SimplestApp object {
				to getOne(): Int {
					val array = Array(Array(2))
					return array.size
				}
			}
			native Array class {
				containing Element
				val size: Int
				native init(iterable: <Element>Iterable)
				bound Iterator class: Identifiable & <Int>IndexIterator & <Element>ValueIterator {
					var index = 0
					overriding computed currentIndex gets index
					overriding computed currentValue: Element gets this<Array>[index]
					overriding computed isDone gets index == size

					overriding to advance() {
						index++
					}
				}
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne", mapOf(
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME,
			SpecialType.STRING to TestUtil.TEST_FILE_NAME
		))
		assertEquals(1, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `can be constructed given a value and repetition count`() {
		val sourceCode = """
			SimplestApp object {
				to getOne(): Int {
					val array = Array(2, 1)
					return array.size
				}
			}
			native Array class {
				containing Element
				val size: Int
				native init(value: Element, size)
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne", mapOf(
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME,
			SpecialType.STRING to TestUtil.TEST_FILE_NAME
		))
		assertEquals(1, Llvm.castToSignedInteger(result))
	}
}
