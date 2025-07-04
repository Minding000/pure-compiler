package components.code_generation.general

import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class ErrorPropagation {

	@Test
	fun `propagates error from function call`() {
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
	fun `propagates error from binary operator`() {
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
	fun `propagates error from unary operator`() {
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
	fun `propagates error from binary modification operator`() {
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
	fun `propagates error from unary modification operator`() {
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

	@Test
	fun `handle block is executed and execution is resumed when error matches in same function`() {
		val sourceCode = """
			SomeError class
			SimplestApp object {
				var x = 0
				to getEleven(): Int {
					x = 1
					raise SomeError()
				} handle SomeError {
					x += 10
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEleven")
		assertEquals(11, result)
	}

	@Test
	fun `handle block is executed and execution is resumed when error matches in parent function`() {
		val sourceCode = """
			SomeError class
			SimplestApp object {
				var x = 0
				operator ++: Int {
					x = 1
					raise SomeError()
					x = 2
				}
				to getEleven(): Int {
					this++
					x = 3
					return x
				} handle SomeError {
					x += 10
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getEleven")
		assertEquals(11, result)
	}

	@Test
	fun `handle block is not executed and error is propagated when error doesn't match in same function`() {
		val sourceCode = """
			SomeError class
			OtherError class
			SimplestApp object {
				var x = 0
				to throw() {
					x = 1
					raise SomeError()
				} handle OtherError {
					x += 10
				}
				to getOne(): Int {
					try? throw()
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne")
		assertEquals(1, result)
	}

	@Test
	fun `handle block is not executed and error is propagated when error doesn't match in parent function`() {
		val sourceCode = """
			SomeError class
			OtherError class
			SimplestApp object {
				var x = 0
				to throw() {
					x = 1
					raise SomeError()
				}
				to propagateException() {
					throw()
					x = 2
				} handle OtherError {
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
	fun `handle block is executed and error is re-raised when error matches and handle block contains raise statement`() {
		val sourceCode = """
			SomeError class
			SimplestApp object {
				var x = 0
				to throw() {
					x = 1
					raise SomeError()
				} handle SomeError {
					x = 2
					raise SomeError()
				}
				to getFour(): Int {
					throw()
					x = 3
					return x
				} handle SomeError {
					x += 2
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFour")
		assertEquals(4, result)
	}

	@Test
	fun `always block is executed and execution is resumed after handle block ran`() {
		val sourceCode = """
			SomeError class
			SimplestApp object {
				to getFour(): Int {
					var x = 0
					{
						x += 1
						raise SomeError()
					} handle SomeError {
						x += 1
					} always {
						x += 1
					}
					x += 1
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFour")
		assertEquals(4, result)
	}

	@Test
	fun `always block is executed and error is propagated after handle block didn't run`() {
		val sourceCode = """
			SomeError class
			OtherError class
			SimplestApp object {
				var x = 0
				to throw() {
					{
						x += 1
						raise SomeError()
					} handle OtherError {
						x = 6
					} always {
						x += 1
					}
					x = 9
				}
				to getTwo(): Int {
					try? throw()
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTwo")
		assertEquals(2, result)
	}

	@Test
	fun `always block is executed after error re-raised in handle block`() {
		val sourceCode = """
			SomeError class
			SimplestApp object {
				var x = 0
				to throw() {
					{
						x += 1
						raise SomeError()
					} handle error: SomeError {
						x += 1
						raise error
					} always {
						x += 1
					}
					x = 9
				}
				to getThree(): Int {
					try? throw()
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getThree")
		assertEquals(3, result)
	}

	@Test
	fun `handle block matches error after another handle block didn't match it`() {
		val sourceCode = """
			SomeError class
			OtherError class
			SimplestApp object {
				var x = 0
				to getTwo(): Int {
					x += 1
					raise SomeError()
				} handle error: OtherError {
					return 44
				} handle error: SomeError {
					x += 1
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getTwo")
		assertEquals(2, result)
	}

	@Test
	fun `always block is executed after return with handle block present`() {
		val sourceCode = """
			SomeError class
			SimplestApp object {
				var x = 0
				to getZero(): Int {
					return x
				} handle error: SomeError {
					return 44
				} always {
					x += 1
				}
				to getOne(): Int {
					return getZero() + x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne")
		assertEquals(1, result)
	}

	@Test
	fun `always block is executed after return without handle blocks`() {
		val sourceCode = """
			SimplestApp object {
				var x = 0
				to getZero(): Int {
					return x
				} always {
					x += 1
				}
				to getOne(): Int {
					return getZero() + x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getOne")
		assertEquals(1, result)
	}

	@Test
	fun `always block is executed after break`() {
		val sourceCode = """
			SimplestApp object {
				to getFive(): Int {
					var x = 4
					loop {
						break
					} always {
						x += 1
					}
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getFive")
		assertEquals(5, result)
	}

	@Test
	fun `always block is executed after next`() {
		val sourceCode = """
			SimplestApp object {
				to getSix(): Int {
					var x = 4
					loop while x < 5 {
						x += 1
						next
						x = 10
					} always {
						x += 1
					}
					return x
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, result)
	}

	@Test
	fun `always block with raise`() {
		val sourceCode = """
			SomeError class
			OtherError class
			SimplestApp object {
				to throw() {
					raise SomeError()
				} always {
					raise OtherError()
				}
				to getSix(): Int {
					throw()
					return 4
				} handle OtherError {
					return 6
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, result)
	}

	@Test
	fun `always block with return`() {
		val sourceCode = """
			SomeError class
			SimplestApp object {
				to getSix(): Int {
					raise SomeError()
				} always {
					return 6
				}
			}
			""".trimIndent()
		val result = TestUtil.run(sourceCode, "Test:SimplestApp.getSix")
		assertEquals(6, result)
	}
}
