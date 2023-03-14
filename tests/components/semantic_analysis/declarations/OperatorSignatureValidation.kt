package components.semantic_analysis.declarations

import logger.Severity
import logger.issues.definition.*
import org.junit.jupiter.api.Test
import util.TestUtil

internal class OperatorSignatureValidation {

	@Test
	fun `emits warning for index operators with parameter and return type`() {
		val sourceCode = """
			Vector class {
				operator [key: IndexType](value: Int): Int
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ReadWriteIndexOperator>(
			"Index operators can not accept and return a value at the same time.", Severity.WARNING)
	}

	@Test
	fun `doesn't emit warning for index operators with either parameter or return type`() {
		val sourceCode = """
			Vector class {
				operator [key: IndexType](): Int
				operator [key: IndexType](value: Int)
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ReadWriteIndexOperator>()
	}

	@Test
	fun `emits warning for binary operators that don't take exactly one parameter`() {
		val sourceCode = """
			Vector class {
				operator -(a: Self, b: Self): ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<BinaryOperatorWithInvalidParameterCount>(
			"Binary operators need to accept exactly one parameter.", Severity.WARNING)
	}

	@Test
	fun `doesn't emit warning for binary operators that take exactly one parameter`() {
		val sourceCode = """
			Vector class {
				operator -(other: Self): ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<BinaryOperatorWithInvalidParameterCount>()
	}

	@Test
	fun `doesn't emit warning for unary operators that don't take exactly one parameter`() {
		val sourceCode = """
			Vector class {
				operator -: ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<BinaryOperatorWithInvalidParameterCount>()
	}

	@Test
	fun `emits warning for unary operators that take parameters`() {
		val sourceCode = """
			Vector class {
				operator !(other: Self): ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ParameterInUnaryOperator>("Unary operators can't accept parameters.", Severity.WARNING)
	}

	@Test
	fun `doesn't emit warning for unary operators that don't take parameters`() {
		val sourceCode = """
			Vector class {
				operator !: ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ParameterInUnaryOperator>()
	}

	@Test
	fun `doesn't emit warning for binary operators that take parameters`() {
		val sourceCode = """
			Vector class {
				operator -(a: Self): ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ParameterInUnaryOperator>()
	}

	@Test
	fun `emits warning for returning operators that don't have a return type`() {
		val sourceCode = """
			Vector class {
				operator !
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OperatorExpectedToReturn>("This operator is expected to return a value.",
			Severity.WARNING)
	}

	@Test
	fun `doesn't emit warning for returning operators that have a return type`() {
		val sourceCode = """
			Vector class {
				operator !: ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OperatorExpectedToReturn>()
		lintResult.assertIssueNotDetected<OperatorExpectedToNotReturn>()
	}

	@Test
	fun `emits warning for non-returning operators that have a return type`() {
		val sourceCode = """
			Vector class {
				operator ++: Vector
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OperatorExpectedToNotReturn>("This operator is not expected to return a value.",
			Severity.WARNING)
	}

	@Test
	fun `doesn't emit warning for non-returning operators that don't have a return type`() {
		val sourceCode = """
			Vector class {
				operator ++
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OperatorExpectedToReturn>()
		lintResult.assertIssueNotDetected<OperatorExpectedToNotReturn>()
	}
}
