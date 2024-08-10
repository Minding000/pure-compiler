package components.code_generation.operations

import components.semantic_model.context.SpecialType
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class Equality {

	@Test
	fun `compares integers`() {
		val sourceCode = """
			SimplestApp object {
				to getNo(): Bool {
					return 2 == 3
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, result)
	}

	@Test
	fun `allows null on the right-hand side`() {
		val sourceCode = """
			Int class {
				operator ==(other: Any?): Bool {
					return no
				}
			}
			SimplestApp object {
				to getNo(): Bool {
					return Int() == null
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getNo")
		assertEquals(false, result)
	}

	@Test
	fun `allows null on the left-hand side`() {
		val sourceCode = """
			Int class
			native Null object {
				native operator ==(other: Any?): Bool
			}
			SimplestApp object {
				to getNo(): Bool {
					return null == Int()
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getNo", mapOf(
			SpecialType.NULL to listOf("Test")
		))
		assertEquals(false, result)
	}

	@Test
	fun `allows optional null on the left-hand side`() {
		val sourceCode = """
			Int class {
				operator ==(other: Any?): Bool {
					return yes
				}
			}
			native Null object {
				native operator ==(other: Any?): Bool
			}
			SimplestApp object {
				to getNo(): Bool {
					val int: Int? = null
					return int == Int()
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getNo", mapOf(
			SpecialType.NULL to listOf("Test")
		))
		assertEquals(false, result)
	}

	@Test
	fun `allows optional value on the left-hand side`() {
		val sourceCode = """
			Int class {
				operator ==(other: Any?): Bool {
					return yes
				}
			}
			native Null object {
				native operator ==(other: Any?): Bool
			}
			SimplestApp object {
				to getNo(): Bool {
					val int: Int? = Int()
					return int == Int()
				}
			}
			""".trimIndent()
		val result = TestUtil.runAndReturnBoolean(sourceCode, "Test:SimplestApp.getNo", mapOf(
			SpecialType.NULL to listOf("Test")
		))
		assertEquals(true, result)
	}
}
