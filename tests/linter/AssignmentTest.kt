package linter

import util.TestUtil
import linter.messages.Message
import org.junit.jupiter.api.Test

internal class AssignmentTest {

	@Test
	fun testIncompatibleTypes() {
		val sourceCode =
			"""
				val a = 5
				var b = "I'm not a number"
				var c = 6
				b = a
				c = a
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Type 'Int' is not assignable to type 'String'.")
		lintResult.assertLinterMessageNotEmitted(Message.Type.ERROR, "Type 'Int' is not assignable to type 'Int'.")
	}

	@Test
	fun complexTypesCanBeAssignedToTypeAlias() {
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
	fun typeAliasesCanBeAssignedToComplexType() {
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