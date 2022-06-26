package linter

import TestUtil
import linter.messages.Message
import org.junit.jupiter.api.Test

internal class ModifierTest {

	@Test
	fun testInvalidModifier() {
		val sourceCode =
			"""
				override class House {}
            """.trimIndent()
		TestUtil.assertLinterMessageEmitted(Message.Type.WARNING, "Modifier 'override' is not allowed here", sourceCode)
	}

	@Test
	fun testDuplicateModifier() {
		val sourceCode =
			"""
				native native class Memory {}
            """.trimIndent()
		TestUtil.assertLinterMessageEmitted(Message.Type.WARNING, "Duplicate 'native' modifier", sourceCode)
	}
}