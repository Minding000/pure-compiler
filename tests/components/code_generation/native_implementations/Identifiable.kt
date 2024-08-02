package components.code_generation.native_implementations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Identifiable {

	@Test
	fun `returns address as string representation`() {
		val sourceCode = """
			SimplestApp object {
				to getAddressLength(): Int {
					return stringRepresentation.characterCount
				}
			}
			abstract Identifiable class {
				native gettable computed stringRepresentation: String
			}
			copied String class {
				computed characterCount: Int
					gets bytes.size
				var bytes: ByteArray
				init(bytes)
			}
			native ByteArray class {
				val size: Int
				native init(value: Byte, size)
			}
		""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getAddressLength", mapOf(
			SpecialType.IDENTIFIABLE to TestUtil.TEST_FILE_NAME,
			SpecialType.STRING to TestUtil.TEST_FILE_NAME,
			SpecialType.BYTE_ARRAY to TestUtil.TEST_FILE_NAME
		))
		assertEquals(16, result)
	}
}
