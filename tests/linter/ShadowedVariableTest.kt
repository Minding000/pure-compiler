package linter

import TestUtil
import linter.messages.Message
import org.junit.jupiter.api.Test

internal class ShadowedVariableTest {

	@Test
	fun testShadowedVariable() {
		val sourceCode =
			"""
				class Handler {}
				val defaultHandler: Handler
				class Event {
					const defaultHandler: Handler
				}
            """.trimIndent()
		TestUtil.assertLinterMessage(Message.Type.WARNING, "'defaultHandler' shadows a variable.", sourceCode)
	}
}