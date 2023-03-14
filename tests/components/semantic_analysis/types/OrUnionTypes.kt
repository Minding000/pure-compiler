package components.semantic_analysis.types

import logger.issues.constant_conditions.TypeNotAssignable
import org.junit.jupiter.api.Test
import util.TestUtil

internal class OrUnionTypes {

	@Test
	fun `or unions can be assigned to or unions`() {
		val sourceCode =
			"""
				Bus class
				Car class
				val preferredVehicle: Bus | Car = Bus()
				val dailyCommuteVehicle: Bus | Car = preferredVehicle
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}
}
