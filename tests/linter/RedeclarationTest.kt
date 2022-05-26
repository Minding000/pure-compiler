package linter

import TestUtil
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
		TestUtil.assertLinterMessage(Message.Type.ERROR, "Redeclaration of value 'car'", sourceCode)
	}

	@Test
	fun testRedeclarationOfType() {
		val sourceCode =
			"""
				class Animal {}
				enum Animal {}
            """.trimIndent()
		TestUtil.assertLinterMessage(Message.Type.ERROR, "Redeclaration of type 'Animal'", sourceCode)
	}
}