package components.semantic_analysis

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Assignability {

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
				Car class {
				}
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

	@Test
	fun `complex types can be assigned to type aliases`() {
		val sourceCode =
			"""
				Event class {}
				alias EventHandler = (Event) =>|
				val complexTypeValue: (Event) =>|
				var typeAliasValue: EventHandler = complexTypeValue
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
	}

	@Test
	fun `type aliases can be assigned to complex types`() {
		val sourceCode =
			"""
				Event class {}
				alias EventHandler = (Event) =>|
				val typeAliasValue: EventHandler
				var complexTypeValue: (Event) =>| = typeAliasValue
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not assignable to type")
	}
}
