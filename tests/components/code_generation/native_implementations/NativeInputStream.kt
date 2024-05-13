package components.code_generation.native_implementations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class NativeInputStream {

	@Test
	fun `'readBytes' doesn't fail`() {
		val sourceCode = """
			SimplestApp object {
				bound Process object {
					val inputStream = getStandardInputStream()
					native to getStandardInputStream(): NativeInputStream
				}
				to getZero(): Int {
					return Process.inputStream.readBytes(0).size
				}
			}
			native ByteArray class {
				val size: Int
			}
			copied String class {
				var bytes: ByteArray
				init(bytes)
			}
			native NativeInputStream class {
				native to readBytes(amount: Int): ByteArray
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getZero", mapOf(
			SpecialType.BYTE_ARRAY to TestUtil.TEST_FILE_NAME,
			SpecialType.STRING to TestUtil.TEST_FILE_NAME,
			SpecialType.NATIVE_INPUT_STREAM to TestUtil.TEST_FILE_NAME,
		))
		assertEquals(0, result)
	}
}
