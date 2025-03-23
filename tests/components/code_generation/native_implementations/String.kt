package components.code_generation.native_implementations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestApp

internal class String {

	@Test
	fun `can be constructed from a Float`() {
		val sourceCode = """
			SimplestApp object {
				bound Process object {
					val outputStream = getStandardOutputStream()
					native to getStandardOutputStream(): NativeOutputStream
				}
				to printFloat() {
					Process.outputStream.writeBytes(String(100.45).bytes)
				}
			}
			native NativeOutputStream class {
				native to writeBytes(bytes: ByteArray)
			}
			native ByteArray class {
				val size: Int
			}
			copied String class {
				var bytes: ByteArray
				init(bytes)
				native converting init(float: Float)
			}
		""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.printFloat")
		app.setSpecialTypeDeclarations(SpecialType.NATIVE_OUTPUT_STREAM, SpecialType.BYTE_ARRAY, SpecialType.STRING)
		app.shouldPrint("100.449997")
	}
}
