package components.semantic_analysis.types

import logger.issues.constant_conditions.TypeNotAssignable
import org.junit.jupiter.api.Test
import util.TestUtil

internal class ObjectTypes {

	@Test
	fun `types object can be assigned to types object`() {
		val sourceCode =
			"""
				Car class
				val car: Car = Car()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}
}
