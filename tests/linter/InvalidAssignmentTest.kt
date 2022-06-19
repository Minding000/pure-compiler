package linter

import TestUtil
import linter.messages.Message
import org.junit.jupiter.api.Test

internal class InvalidAssignmentTest {

	@Test
	fun testUndeclaredValue() {
		val sourceCode =
			"""
				val a = 5
				var b = "I'm not a number"
				b = a
            """.trimIndent()
		TestUtil.includeRequiredModules = true
		TestUtil.assertLinterMessage(Message.Type.ERROR, "Type 'Int' is not assignable to type 'String'.", sourceCode)
	}
}