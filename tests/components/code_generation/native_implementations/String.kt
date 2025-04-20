package components.code_generation.native_implementations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import util.TestApp

internal class String {

	@TestFactory
	fun `can be constructed from a Float`() = listOf(
		100.45f to "100.449997",
		7f to "7",
		-2f to "-2",
		0f to "0",
	).map { (float, string) ->
		DynamicTest.dynamicTest("String representation of float $float should be $string") {
			val sourceCode = """
				App object {
					bound Process object {
						val outputStream = getStandardOutputStream()
						native to getStandardOutputStream(): NativeOutputStream
					}
					to printFloat() {
						Process.outputStream.writeBytes(String($float).bytes)
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
			val app = TestApp(sourceCode, "Test:App.printFloat")
			app.setSpecialTypeDeclarations(SpecialType.NATIVE_OUTPUT_STREAM, SpecialType.BYTE_ARRAY, SpecialType.STRING)
			app.shouldPrint(string)
		}
	}
}
