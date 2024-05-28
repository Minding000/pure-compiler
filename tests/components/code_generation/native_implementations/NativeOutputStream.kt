package components.code_generation.native_implementations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestApp

internal class NativeOutputStream {

	@Test
	fun `'writeByte' prints to output`() {
		val sourceCode = """
			SimplestApp object {
				bound Process object {
					val outputStream = getStandardOutputStream()
					native to getStandardOutputStream(): NativeOutputStream
				}
				to printTest() {
					Process.outputStream.writeByte(48)
					Process.outputStream.writeByte(49)
					Process.outputStream.writeByte(50)
				}
			}
			native NativeOutputStream class {
				native to writeByte(byte: Byte)
			}
		""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.printTest")
		app.setSpecialTypeDeclarations(SpecialType.NATIVE_OUTPUT_STREAM)
		app.shouldPrint("012")
	}

	@Test
	fun `'writeBytes' prints to output`() {
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
		val app = TestApp(sourceCode, "Test:SimplestApp.printTest")
		app.setSpecialTypeDeclarations(SpecialType.BYTE_ARRAY, SpecialType.STRING, SpecialType.NATIVE_OUTPUT_STREAM)
		app.shouldPrint("Test")
	}
}
