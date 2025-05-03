package components.code_generation.native_implementations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
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

	@TestFactory
	fun `raises an exception when parsing an invalid String`() = listOf(
		"abc" to "a",
		"2:3" to ":",
		"-+1" to "-",
		"123,456" to ",",
		"12.34.56" to ".",
	).map { (string, invalidCharacter) ->
		DynamicTest.dynamicTest("Float parsed from $string should be $invalidCharacter") {
			val sourceCode = """
				referencing Pure
				App object {
					to parseFloat(): Float {
						return Float("$string")
					}
				}
			""".trimIndent()
			val app = TestApp(sourceCode, "Test:App.parseFloat")
			app.includeRequiredModules = true
			val expectedOutput = """
				Unhandled error: Failed to parse float: Invalid character '$invalidCharacter'
				 at Pure:Float:17:Float(String)
				 at Test:Test:4:App.parseFloat(): Float
				""".trimIndent()
			app.shouldPrintLine(expectedOutput, "", 1)
		}
	}

	@Test
	fun `raises an exception when parsing an empty String`() {
		val sourceCode = """
				referencing Pure
				App object {
					to parseFloat(): Float {
						return Float("")
					}
				}
			""".trimIndent()
		val app = TestApp(sourceCode, "Test:App.parseFloat")
		app.includeRequiredModules = true
		val expectedOutput = """
				Unhandled error: Failed to parse float: Empty string
				 at Pure:Float:17:Float(String)
				 at Test:Test:4:App.parseFloat(): Float
				""".trimIndent()
		app.shouldPrintLine(expectedOutput, "", 1)
	}
}
