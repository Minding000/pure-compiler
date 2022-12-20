package components.semantic_analysis.types

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class OptionalTypes {

	@Test
	fun `types object can be assigned to optional types`() {
		val sourceCode =
			"""
				Car class {
					init
				}
				val car: Car? = Car()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
	}

	@Test
	fun `null can be assigned to optional types`() {
		val sourceCode =
			"""
				Car class
				val car: Car? = null
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
	}

	@Test
	fun `optional types can be assigned to optional types`() {
		val sourceCode =
			"""
				Car class {
					init
				}
				val carInDriveway: Car? = Car()
				val car: Car? = carInDriveway
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
	}
}
