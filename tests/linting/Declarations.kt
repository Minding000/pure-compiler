package linting

import util.TestUtil
import linting.messages.Message
import org.junit.jupiter.api.Test

internal class Declarations {

	@Test
	fun `detects shadowed variables`() {
		val sourceCode =
			"""
				class Handler {}
				val defaultHandler: Handler
				class Event {
					const defaultHandler: Handler
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageEmitted(Message.Type.WARNING, "'defaultHandler' shadows a variable.")
	}

	@Test
	fun `detects redeclarations of variables`() {
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
	fun `detects redeclarations of types`() {
		val sourceCode =
			"""
				class Animal {}
				enum Animal {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageEmitted(Message.Type.ERROR, "Redeclaration of type 'Animal'")
	}

	@Test
	fun `emits warning for invalid modifiers`() {
		val sourceCode =
			"""
				override class House {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageEmitted(Message.Type.WARNING, "Modifier 'override' is not allowed here")
	}

	@Test
	fun `emits warning for duplicate modifiers`() {
		val sourceCode =
			"""
				native native class Memory {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertLinterMessageEmitted(Message.Type.WARNING, "Duplicate 'native' modifier")
	}
}