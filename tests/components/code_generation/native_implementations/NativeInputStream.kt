package components.code_generation.native_implementations

import components.code_generation.llvm.Llvm
import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class NativeInputStream {

	@Test
	fun `'readBytes' doesn't fail`() {
		val sourceCode = """
			SimplestApp object {
				to getZero(): Int {
					return NativeInputStream(1).readBytes(0).size
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
			NativeInputStream class {
				val identifier: Int
				init(identifier)
				native to readBytes(amount: Int): <Byte>Array
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getZero", mapOf(
			SpecialType.ARRAY to TestUtil.TEST_FILE_NAME,
			SpecialType.STRING to TestUtil.TEST_FILE_NAME
		))
		assertEquals(0, Llvm.castToSignedInteger(result))
	}
}
