package linting

import util.TestUtil
import linting.messages.Message
import org.junit.jupiter.api.Test

internal class Assignments {

	@Test
	fun `emits error for incompatible source expression type`() {
		val sourceCode =
			"""
				val a = 5
				var b = "I'm not a number"
				b = a
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Type 'Int' is not assignable to type 'String'.")
	}

	@Test
	fun `emits error for assignment to constant target variable`() {
		val sourceCode =
			"""
				val a = 5
				val b = 4
				b = a
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "'b' cannot be reassigned, because it is constant.")
	}

	@Test
	fun `complex types can be assigned to type aliases`() {
		val sourceCode =
			"""
				class Event {}
				alias EventHandler = (Event) =>|
				var typeAliasValue: EventHandler
				val complexTypeValue: (Event) =>|
				typeAliasValue = complexTypeValue
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageNotEmitted(Message.Type.ERROR, "Type '(Event) =>|' is not assignable to type 'EventHandler'")
	}

	@Test
	fun `type aliases can be assigned to complex types`() {
		val sourceCode =
			"""
				class Event {}
				alias EventHandler = (Event) =>|
				val typeAliasValue: EventHandler
				var complexTypeValue: (Event) =>|
				complexTypeValue = typeAliasValue
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageNotEmitted(Message.Type.ERROR, "Type 'EventHandler' is not assignable to type '(Event) =>|'")
	}
}