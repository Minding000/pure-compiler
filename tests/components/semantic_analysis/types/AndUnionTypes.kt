package components.semantic_analysis.types

import logger.issues.constant_conditions.TypeNotAssignable
import org.junit.jupiter.api.Test
import util.TestUtil

internal class AndUnionTypes {

	@Test
	fun `and unions can be assigned to and unions`() {
		val sourceCode =
			"""
				StreetVehicle class
				PublicTransport class
				Bus object: StreetVehicle & PublicTransport
				val preferredVehicle: StreetVehicle & PublicTransport = Bus
				val dailyCommuteVehicle: StreetVehicle & PublicTransport = preferredVehicle
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}
}
