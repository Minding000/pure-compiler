package components.semantic_analysis.types

import logger.issues.constant_conditions.TypeNotAssignable
import org.junit.jupiter.api.Test
import util.TestUtil

internal class ObjectTypes {

	@Test
	fun `object types can be assigned to themselves`() {
		val sourceCode =
			"""
				val car: Car = Car()
				Car class
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `object types can be assigned to their super types`() {
		val sourceCode =
			"""
				val car: Car
				val vehicle: Vehicle = car
				Car class: Vehicle
				Vehicle class
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}
}
