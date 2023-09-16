package components.semantic_model.types

import logger.issues.constant_conditions.TypeNotAssignable
import org.junit.jupiter.api.Test
import util.TestUtil

internal class OptionalTypes {

	@Test
	fun `types object can be assigned to optional types`() {
		val sourceCode =
			"""
				Car class
				val car: Car? = Car()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `null can be assigned to optional types`() {
		val sourceCode =
			"""
				Car class
				val car: Car? = null
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `optional types can be assigned to optional types`() {
		val sourceCode =
			"""
				Car class
				val carInDriveway: Car? = Car()
				val car: Car? = carInDriveway
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}
}
