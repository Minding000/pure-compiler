package components.semantic_model.operations

import logger.Severity
import logger.issues.constant_conditions.ExpressionNotAssignable
import logger.issues.constant_conditions.TypeNotAssignable
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Assignments {

	@Test
	fun `emits error for incompatible source expression type`() {
		val sourceCode =
			"""
				Int class
				String class
				val a = Int()
				var b = String()
				b = a
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<TypeNotAssignable>("Type 'Int' is not assignable to type 'String'.", Severity.ERROR)
	}

	@Test
	fun `allows for integers to be assigned to floats`() {
		val sourceCode =
			"""
				val a = 5
				var b = 6.2
				b = a
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
		lintResult.assertIssueNotDetected<ExpressionNotAssignable>()
	}

	@Test
	fun `allows for generic types to be assigned to their base type`() {
		val sourceCode =
			"""
				Number class
				Cube class {
					containing N: Number
					val sideLength: N
					to getSideLength(): Number {
						val sideLengthAsNumber: Number = sideLength
						return sideLengthAsNumber
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
		lintResult.assertIssueNotDetected<ExpressionNotAssignable>()
	}
}
