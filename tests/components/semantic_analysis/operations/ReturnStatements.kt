package components.semantic_analysis.operations

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class ReturnStatements {

	@Test
	fun `detects return statements outside of functions`() {
		val sourceCode =
			"""
				return
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Return statements are not allowed outside of functions")
	}

	@Test
	fun `detects return statements inside of initializers`() {
		val sourceCode =
			"""
				Plane class {
					init {
						return
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Return statements are not allowed outside of functions")
	}

	@Test
	fun `ignores return statements inside of functions`() {
		val sourceCode =
			"""
				Desk class {
					to raiseToMaximum() {
						return
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Return statements are not allowed outside of functions")
	}

	@Test
	fun `detects return statements with incorrect return type`() {
		val sourceCode =
			"""
				Int class
				Desk class {
					init
					to getMaximumHeightInMeters(): Int {
						return Desk()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Return value doesn't match the declared return type")
	}

	@Test
	fun `detects return statements with missing return value`() {
		val sourceCode =
			"""
				Int class
				Desk class {
					to getMaximumHeightInMeters(): Int {
						return
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Return value doesn't match the declared return type")
	}

	@Test
	fun `detects return statements with extraneous return type`() {
		val sourceCode =
			"""
				Int class {
					init
				}
				Desk class {
					to raiseToMaximum() {
						return Int()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Return value doesn't match the declared return type")
	}

	@Test
	fun `ignores return statements with correct return type`() {
		val sourceCode =
			"""
				Number class
				Int class: Number {
					init
				}
				Desk class {
					to getMaximumHeightInMeters(): Number {
						return Int()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,"Return value doesn't match the declared return type")
	}

	@Test
	fun `ignores return statements with absent return type and value`() {
		val sourceCode =
			"""
				Desk class {
					to raiseToMaximum() {
						return
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,"Return value doesn't match the declared return type")
	}

	@Test
	fun `detects functions with return type that might complete without returning a value in the main block`() {
		val sourceCode =
			"""
				Int class
				Desk class {
					to raiseToMaximum(): Int {
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Function might complete without returning a value")
	}

	@Test
	fun `detects functions with return type that might complete without returning a value in a handle block`() {
		val sourceCode =
			"""
				Error class {
					init
				}
				Int class
				Desk class {
					to raiseToMaximum(): Int {
						raise Error()
					} handle Error {
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Function might complete without returning a value")
	}

	@Test
	fun `detects functions that don't return`() {
		val sourceCode =
			"""
				Error class {
					init
				}
				Int class
				Desk class {
					to raiseToMaximum(): Int {
						raise Error()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Function never returns")
	}
}
