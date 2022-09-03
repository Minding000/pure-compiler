package linter

import util.TestUtil
import linter.messages.Message
import org.junit.jupiter.api.Test

internal class ModifierTest {

	@Test
	fun testInvalidModifier() {
		val sourceCode =
			"""
				override class House {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageEmitted(Message.Type.WARNING, "Modifier 'override' is not allowed here")
	}

	@Test
	fun testDuplicateModifier() {
		val sourceCode =
			"""
				native native class Memory {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageEmitted(Message.Type.WARNING, "Duplicate 'native' modifier")
	}
}