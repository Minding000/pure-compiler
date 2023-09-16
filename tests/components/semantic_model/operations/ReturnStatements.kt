package components.semantic_model.operations

import logger.Severity
import logger.issues.constant_conditions.FunctionCompletesDespiteNever
import logger.issues.constant_conditions.FunctionCompletesWithoutReturning
import logger.issues.returns.RedundantReturnValue
import logger.issues.returns.ReturnStatementMissingValue
import logger.issues.returns.ReturnStatementOutsideOfCallable
import logger.issues.returns.ReturnValueTypeMismatch
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
		lintResult.assertIssueDetected<ReturnStatementOutsideOfCallable>(
			"Return statements are not allowed outside of callables.", Severity.ERROR)
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
		lintResult.assertIssueDetected<ReturnStatementOutsideOfCallable>()
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
		lintResult.assertIssueNotDetected<ReturnStatementOutsideOfCallable>()
	}

	@Test
	fun `detects return statements with incorrect return type`() {
		val sourceCode =
			"""
				Int class
				Desk class {
					to getMaximumHeightInMeters(): Int {
						return Desk()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ReturnValueTypeMismatch>(
			"The type 'Desk' of the returned value doesn't match the declared return type 'Int'.", Severity.ERROR)
	}

	@Test
	fun `ignores return statements with correct return type`() {
		val sourceCode =
			"""
				Number class
				Int class: Number
				Desk class {
					to getMaximumHeightInMeters(): Number {
						return Int()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ReturnValueTypeMismatch>()
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
		lintResult.assertIssueNotDetected<ReturnValueTypeMismatch>()
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
		lintResult.assertIssueDetected<ReturnStatementMissingValue>("Return statement needs a value.", Severity.ERROR)
	}

	@Test
	fun `detects return statements with redundant return type`() {
		val sourceCode =
			"""
				Int class
				Desk class {
					to raiseToMaximum() {
						return Int()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<RedundantReturnValue>("Return value is redundant.", Severity.WARNING)
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
		lintResult.assertIssueDetected<FunctionCompletesWithoutReturning>("Function might complete without returning a value.",
			Severity.ERROR)
	}

	@Test
	fun `detects functions with return type that might complete without returning a value in a handle block`() {
		val sourceCode =
			"""
				Error class
				Int class
				Desk class {
					to raiseToMaximum(): Int {
						raise Error()
					} handle Error {
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<FunctionCompletesWithoutReturning>()
	}

	@Test
	fun `detects functions with never return type that complete`() {
		val sourceCode =
			"""
				Desk class {
					to raiseToMaximum(): Never {
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<FunctionCompletesDespiteNever>("Function might complete despite of 'Never' return type.",
			Severity.ERROR)
	}

	@Test
	fun `detects functions with never return type that return`() {
		val sourceCode =
			"""
				Desk class {
					to raiseToMaximum(): Never {
						return
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<FunctionCompletesDespiteNever>()
	}

	@Test
	fun `ignores functions with never return type that don't complete`() {
		val sourceCode =
			"""
				Error class
				Desk class {
					to raiseToMaximum(): Never {
						raise Error()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<FunctionCompletesDespiteNever>()
	}
}
