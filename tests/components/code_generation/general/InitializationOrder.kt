package components.code_generation.general

import components.code_generation.llvm.Llvm
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class InitializationOrder {

	@Test
	fun `initializes classes before objects in one file`() {
		val sourceCode = """
			SimplestApp object: Application {
				to getEightyFour(): Int {
					return Process.a
				}
			}
			Application class {
				bound Process object {
					val a = 84
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEightyFour")
		assertEquals(84, Llvm.castToSignedInteger(result))
	}

	@Test
	fun `initializes classes before objects over multiple files`() {
		val simplestAppSourceCode = """
			referencing ${TestUtil.TEST_MODULE_NAME}
			SimplestApp object: Application {
				to getEightyFour(): Int {
					return Process.a
				}
			}
			""".trimIndent()
		val applicationSourceCode = """
			Application class {
				bound Process object {
					val a = 84
				}
			}
			""".trimIndent()
		val result = TestUtil.run(mapOf(
			"SimplestApp" to simplestAppSourceCode,
			"Application" to applicationSourceCode,
		), "Test:SimplestApp.getEightyFour")
		assertEquals(84, Llvm.castToSignedInteger(result))
	}
}
