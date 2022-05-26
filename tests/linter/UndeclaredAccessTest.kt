package linter

import TestUtil
import linter.messages.Message
import org.junit.jupiter.api.Test

internal class UndeclaredAccessTest {

	@Test
	fun testUndeclaredValue() {
		val sourceCode =
			"""
				val square = x * x
            """.trimIndent()
		TestUtil.assertLinterMessage(Message.Type.ERROR, "Value 'x' hasn't been declared yet.", sourceCode)
	}

	@Test
	fun testUndeclaredType() {
		val sourceCode =
			"""
				val defaultHandler: Handler
            """.trimIndent()
		TestUtil.assertLinterMessage(Message.Type.ERROR, "Type 'Handler' hasn't been declared yet.", sourceCode)
	}
}