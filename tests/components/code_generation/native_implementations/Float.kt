package components.code_generation.native_implementations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import util.TestApp

internal class Float {

	@TestFactory
	fun `can be constructed from a String`() = listOf(
		"100.449997" to 100.45f,
		"7" to 7f,
		"-2" to -2f,
		"0" to 0f,
	).map { (string, float) ->
		DynamicTest.dynamicTest("Float parsed from $string should be $float") {
			val sourceCode = """
				App object {
					to parseFloat(): Float {
						return Float("$string")
					}
				}
				native ByteArray class {
					val size: Int
				}
				copied String class {
					var bytes: ByteArray
					init(bytes)
				}
				native copied Float class {
					native init(decimalString: String)
				}
			""".trimIndent()
			val app = TestApp(sourceCode, "Test:App.parseFloat")
			app.setSpecialTypeDeclarations(SpecialType.BYTE_ARRAY, SpecialType.STRING, SpecialType.FLOAT)
			app.shouldExitWith(float)
		}
	}
}
