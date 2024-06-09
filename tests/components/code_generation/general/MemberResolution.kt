package components.code_generation.general

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class MemberResolution {

	@Test
	fun `resolves members`() {
		val sourceCode = """
			SimplestApp object {
				val a = 62
				to getA(): Int {
					return SimplestApp.a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(62, result)
	}

	@Test
	fun `resolves overridden members`() {
		val sourceCode = """
			Parent class {
				val a = 63
				to getThing(): Parent {
					return this
				}
			}
			SimplestApp object: Parent {
				overriding to getThing(): SimplestApp {
					return this
				}
				to getA(): Int {
					return SimplestApp.getThing().a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(63, result)
	}

	@Test
	fun `resolves implemented abstract members`() {
		val sourceCode = """
			abstract Parent class {
				val a = 64
				abstract to getThing(): Parent
			}
			SimplestApp object: Parent {
				overriding to getThing(): SimplestApp {
					return this
				}
				to getA(): Int {
					return SimplestApp.getThing().a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(64, result)
	}

	@Test
	fun `resolves implemented generic members`() {
		val sourceCode = """
			Some class {
				val a = 64
			}
			Something object: Some
			abstract Parent class {
				containing T
				abstract to getThing(): T
			}
			SimplestApp object: <Some>Parent {
				overriding to getThing(): Something {
					return Something
				}
				to getA(): Int {
					return SimplestApp.getThing().a
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getA")
		assertEquals(64, result)
	}
}
