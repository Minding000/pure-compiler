package components.code_generation.general

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class ExceptionPropagation {

	@Test
	fun `propagates exception from function call`() {
		val sourceCode = """
			SimplestApp object {
				var x = 0
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
				var x = 0
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
				var x = 0
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
				var x = 0
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
				var x = 0
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

	//TODO write tests:
	// -X exception matches, is handled and execution is resumed in same function
	// -X exception matches, is handled and execution is resumed in parent function
	// - exception doesn't match and is propagated in same function
	// - exception doesn't match and is propagated in parent function
	// - exception matches, is handled and re-raised
	// - exception matches, is handled, always block runs and execution is resumed
	// - exception doesn't match, always block runs and is exception is propagated
	// - exception matches, is handled, re-raised and always block runs
	// - compiles with multiple handle blocks
	// - always block runs on return when handle block is present in same function
	// - always block runs on return when handle block is present in parent function
	// - always block runs on return when handle blocks are absent in same function
	// - always block runs on return when handle blocks are absent in parent function

	//TODO use 'error' parameter
	@Test
	fun `handle block is executed and execution is resumed when exception matches is same function`() {
		val sourceCode = """
			SimplestApp object {
				var x = 0
				to getEleven(): Int {
					x = 1
					raise 23
				} handle error {
					x += 10
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEleven")
		assertEquals(11, result)
	}

	//TODO use 'error' parameter
	@Test
	fun `handle block is executed and execution is resumed when exception matches is parent function`() {
		val sourceCode = """
			SimplestApp object {
				var x = 0
				operator ++: Int {
					x = 1
					raise 23
					x = 2
				}
				to getEleven(): Int {
					this++
					x = 3
					return x
				} handle error {
					x += 10
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEleven")
		assertEquals(11, result)
	}
}
