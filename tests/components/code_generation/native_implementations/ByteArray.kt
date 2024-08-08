package components.code_generation.native_implementations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class ByteArray {

	@Test
	fun `plural type initializer sets size`() {
		val sourceCode = """
			SimplestApp object {
				to getOne(): Int {
					val array = ByteArray(3, 2, 1)
					return array.size
				}
			}
			native ByteArray class {
				val size: Int
				native init(...values: ...Byte)
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne", mapOf(
			SpecialType.BYTE_ARRAY to TestUtil.TEST_FILE_NAME
		))
		assertEquals(3, result)
	}

	@Test
	fun `plural type initializer sets values`() {
		val sourceCode = """
			SimplestApp object {
				to getThree(): Byte {
					val array = ByteArray(3, 2, 1)
					return array[0]
				}
			}
			native ByteArray class {
				val size: Int
				native init(...values: ...Byte)
				native operator [index: Int]: Byte
			}
		""".trimIndent()
		val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getThree", mapOf(
			SpecialType.BYTE_ARRAY to listOf(TestUtil.TEST_FILE_NAME)
		))
		assertEquals(3, result)
	}

	@Test
	fun `value to be repeated initializer sets size`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					val array = ByteArray(2, 5)
					return array.size
				}
			}
			native ByteArray class {
				val size: Int
				native init(value: Byte, size)
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive", mapOf(
			SpecialType.BYTE_ARRAY to TestUtil.TEST_FILE_NAME
		))
		assertEquals(5, result)
	}

	@Test
	fun `value to be repeated initializer sets values`() {
		val sourceCode = """
			SimplestApp object {
				to getSeven(): Byte {
					val array = ByteArray(7, 4)
					return array[3]
				}
			}
			native ByteArray class {
				val size: Int
				native init(value: Byte, size)
				native operator [index: Int]: Byte
			}
		""".trimIndent()
		val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getSeven", mapOf(
			SpecialType.BYTE_ARRAY to listOf(TestUtil.TEST_FILE_NAME)
		))
		assertEquals(7, result)
	}

	@Test
	fun `addition operator sets size`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					val a = ByteArray(7, 2)
					val b = ByteArray(3, 3)
					return (a + b).size
				}
			}
			native ByteArray class {
				val size: Int
				native init(value: Byte, size)
				native operator +(right: ByteArray): ByteArray
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive", mapOf(
			SpecialType.BYTE_ARRAY to TestUtil.TEST_FILE_NAME
		))
		assertEquals(5, result)
	}

	@Test
	fun `addition operator sets values`() {
		val sourceCode = """
			SimplestApp object {
				to getThree(): Byte {
					val a = ByteArray(7, 2)
					val b = ByteArray(3, 3)
					return (a + b)[4]
				}
			}
			native ByteArray class {
				val size: Int
				native init(value: Byte, size)
				native operator [index: Int]: Byte
				native operator +(right: ByteArray): ByteArray
			}
		""".trimIndent()
		val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getThree", mapOf(
			SpecialType.BYTE_ARRAY to listOf(TestUtil.TEST_FILE_NAME)
		))
		assertEquals(3, result)
	}

	@Test
	fun `get operator gets requested value`() {
		val sourceCode = """
			SimplestApp object {
				to getThree(): Byte {
					return ByteArray(1, 2, 3, 4)[2]
				}
			}
			native ByteArray class {
				val size: Int
				native init(...values: ...Byte)
				native operator [index: Int]: Byte
			}
		""".trimIndent()
		val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getThree", mapOf(
			SpecialType.BYTE_ARRAY to listOf(TestUtil.TEST_FILE_NAME)
		))
		assertEquals(3, result)
	}

	@Test
	fun `set operator sets specified value`() {
		val sourceCode = """
			SimplestApp object {
				to getFourteen(): Byte {
					val bytes = ByteArray(1, 2, 3, 4)
					bytes[3] = 14
					bytes[2] = -1
					return bytes[3]
				}
			}
			native ByteArray class {
				val size: Int
				native init(...values: ...Byte)
				native operator [index: Int]: Byte
				native operator [index: Int](element: Byte)
			}
		""".trimIndent()
		val result = TestUtil.runAndReturnByte(sourceCode, "Test:SimplestApp.getFourteen", mapOf(
			SpecialType.BYTE_ARRAY to listOf(TestUtil.TEST_FILE_NAME)
		))
		assertEquals(14, result)
	}
}
