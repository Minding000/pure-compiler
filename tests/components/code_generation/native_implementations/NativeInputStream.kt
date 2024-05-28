package components.code_generation.native_implementations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestApp

internal class NativeInputStream {

	@Test
	fun `'readByte' reads from input`() {
		val sourceCode = """
			SimplestApp object {
				bound Process object {
					val inputStream = getStandardInputStream()
					native to getStandardInputStream(): NativeInputStream
				}
				to getFiftySix(): Byte {
					return Process.inputStream.readByte()
				}
			}
			native NativeInputStream class {
				native to readByte(): Byte
			}
		""".trimIndent()
		val app = TestApp(sourceCode, "Test:SimplestApp.getFiftySix")
		app.setSpecialTypeDeclarations(SpecialType.NATIVE_INPUT_STREAM)
		app.shouldExitWith(56, "8")
	}

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
		val app = TestApp(sourceCode, "Test:SimplestApp.getZero")
		app.setSpecialTypeDeclarations(SpecialType.BYTE_ARRAY, SpecialType.STRING, SpecialType.NATIVE_INPUT_STREAM)
		app.shouldExitWith(0)
	}
}
