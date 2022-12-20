package components.semantic_analysis.types

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class ObjectTypes {

	@Test
	fun `types object can be assigned to types object`() {
		val sourceCode =
			"""
				Car class {
					init
				}
				val car: Car = Car()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
	}
}
