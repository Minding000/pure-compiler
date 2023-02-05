package components.semantic_analysis.types

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class AndUnionTypes {

	@Test
	fun `and unions can be assigned to and unions`() {
		val sourceCode =
			"""
				StreetVehicle class
				PublicTransport class
				Bus object: StreetVehicle & PublicTransport {}
				val preferredVehicle: StreetVehicle & PublicTransport = Bus
				val dailyCommuteVehicle: StreetVehicle & PublicTransport = preferredVehicle
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
	}
}
