package components.code_generation.operations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestApp
import util.TestUtil
import kotlin.test.assertEquals

internal class TemplateStrings {

	@Test
	fun `compiles strings without templates`() {
		val sourceCode = """
			SimplestApp object {
				to getTwelve(): Int {
					return "Hello world!".byteCount
				}
			}
			native ByteArray class {
				val size: Int
			}
			copied String class {
				var bytes: ByteArray
				init(bytes)
				computed byteCount: Int
					gets bytes.size
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTwelve", listOf(SpecialType.BYTE_ARRAY, SpecialType.STRING))
		assertEquals(12, result)
	}

	@Test
	fun `compiles strings with templates`() {
		val sourceCode = """
			referencing Pure
			App object: Application {
				var a = 11
				to printSum() {
					val type = "sum"
					a = 5
					val b = -3
					val answer = "The {type} of {a} and {b} is: {a + b}"
					Process.outputStream.writeBytes(answer.bytes)
				}
			}
		""".trimIndent()
		val app = TestApp(sourceCode, "Test:App.printSum")
		app.includeRequiredModules = true
		app.shouldPrint("The sum of 5 and -3 is: 2")
	}
}
