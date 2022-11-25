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
				val car: Car = new Car()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Type 'Car' is not assignable to type 'Car'")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Type 'Car' is not assignable to type 'Car?'")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Type 'Null' is not assignable to type 'Car?'")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Type 'Car?' is not assignable to type 'Car?'")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Type '(Event) =>|' is not assignable to type 'EventHandler'")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Type 'EventHandler' is not assignable to type '(Event) =>|'")
	}
}
