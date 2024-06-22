package components.code_generation.general

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class ExceptionPropagation {

	@Test
	fun `propagates exception from function call`() {
		val sourceCode = """
			SimplestApp object {
				val x = 0
				to throw() {
					x = 1
					raise 23
					x = 2
				}
				to propagateException() {
					throw()
					x = 3
				}
				to getOne(): Int {
					try? propagateException()
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne")
		assertEquals(1, result)
	}

	@Test
	fun `propagates exception from binary operator`() {
		val sourceCode = """
			SimplestApp object {
				val x = 0
				operator +(app: SimplestApp): Int {
					x = 1
					raise 23
					x = 2
				}
				to propagateException() {
					this + this
					x = 3
				}
				to getOne(): Int {
					try? propagateException()
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne")
		assertEquals(1, result)
	}

	@Test
	fun `propagates exception from unary operator`() {
		val sourceCode = """
			SimplestApp object {
				val x = 0
				operator !: Int {
					x = 1
					raise 23
					x = 2
				}
				to propagateException() {
					!this
					x = 3
				}
				to getOne(): Int {
					try? propagateException()
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne")
		assertEquals(1, result)
	}

	@Test
	fun `propagates exception from binary modification operator`() {
		val sourceCode = """
			SimplestApp object {
				val x = 0
				operator +=(app: SimplestApp) {
					x = 1
					raise 23
					x = 2
				}
				to propagateException() {
					this += this
					x = 3
				}
				to getOne(): Int {
					try? propagateException()
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne")
		assertEquals(1, result)
	}

	@Test
	fun `propagates exception from unary modification operator`() {
		val sourceCode = """
			SimplestApp object {
				val x = 0
				operator ++: Int {
					x = 1
					raise 23
					x = 2
				}
				to propagateException() {
					this++
					x = 3
				}
				to getOne(): Int {
					try? propagateException()
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne")
		assertEquals(1, result)
	}
}
