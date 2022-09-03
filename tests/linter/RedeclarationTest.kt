package linter

import util.TestUtil
import linter.messages.Message
import org.junit.jupiter.api.Test

internal class RedeclarationTest {

	@Test
	fun testRedeclarationOfValue() {
		val sourceCode =
			"""
				class Car {}
				var car: Car
				val car: Car
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Redeclaration of value 'car'")
	}

	@Test
	fun testRedeclarationOfType() {
		val sourceCode =
			"""
				class Animal {}
				enum Animal {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Redeclaration of type 'Animal'")
	}
}