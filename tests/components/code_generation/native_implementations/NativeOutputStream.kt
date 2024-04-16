package components.code_generation.native_implementations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestUtil

internal class NativeOutputStream {

	@Test
	fun `'writeBytes' doesn't fail`() {
		val sourceCode = """
			SimplestApp object {
				to printTest() {
					NativeOutputStream(1).writeBytes("Test".bytes)
				}
			}
			native Array class {
				containing Element
				val size: Int
			}
			copied String class {
				var bytes: <Byte>Array
				init(bytes)
			}
			NativeOutputStream class {
				val identifier: Int
				init(identifier)
				native to writeBytes(bytes: <Byte>Array)
			}
		""".trimIndent()
		TestUtil.run(sourceCode, "Test:SimplestApp.printTest", mapOf(
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME,
			SpecialType.STRING to TestUtil.TEST_FILE_NAME
		))
	}
}
