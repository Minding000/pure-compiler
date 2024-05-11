package components.code_generation.native_implementations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestUtil

internal class NativeOutputStream {

	@Test
	fun `'writeBytes' doesn't fail`() {
		val sourceCode = """
			SimplestApp object {
				bound Process object {
					val outputStream = getStandardOutputStream()
					native to getStandardOutputStream(): NativeOutputStream
				}
				to printTest() {
					Process.outputStream.writeBytes("Test".bytes)
				}
			}
			native ByteArray class {
				val size: Int
			}
			copied String class {
				var bytes: ByteArray
				init(bytes)
			}
			native NativeOutputStream class {
				native to writeBytes(bytes: ByteArray)
			}
		""".trimIndent()
		TestUtil.run(sourceCode, "Test:SimplestApp.printTest", mapOf(
			SpecialType.BYTE_ARRAY to TestUtil.TEST_FILE_NAME,
			SpecialType.STRING to TestUtil.TEST_FILE_NAME,
			SpecialType.NATIVE_OUTPUT_STREAM to TestUtil.TEST_FILE_NAME,
		))
	}
}
