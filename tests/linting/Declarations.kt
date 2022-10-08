package linting

import linting.semantic_model.values.VariableValue
import util.TestUtil
import messages.Message
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

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
		lintResult.assertMessageEmitted(Message.Type.WARNING, "'defaultHandler' shadows a variable.")
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
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Redeclaration of value 'car'")
	}

	@Test
	fun `detects redeclarations of types`() {
		val sourceCode =
			"""
				class Animal {}
				enum Animal {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Redeclaration of type 'Animal'")
	}

	@Test
	fun `detects redeclarations of initializers signatures`() {
		val sourceCode =
			"""
				class Human {
					init
					init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Redeclaration of initializer 'Human()'")
	}

	@Test
	fun `detects redeclarations of function signatures`() {
		val sourceCode =
			"""
				native class Pressure {}
				class Human {
					to sit(): Pressure {}
					to sit() {}
					to sit() {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Redeclaration of function 'sit()'")
	}

	@Test
	fun `detects redeclarations of operator signatures`() {
		val sourceCode =
			"""
				native class Time {}
				alias T = Time
				class Human {
					operator[time: T]() {}
					operator[time: Time]() {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Redeclaration of operator '[Time]()'")
	}

	@Test
	fun `detects invalid modifiers`() {
		val sourceCode =
			"""
				overriding class House {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Modifier 'overriding' is not allowed here")
	}

	@Test
	fun `detects duplicate modifiers`() {
		val sourceCode =
			"""
				native native class Memory {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Duplicate 'native' modifier")
	}

	@Test
	fun `handle block declares error variable`() {
		val sourceCode =
			"""
				native class IOError {}
				class Config {
					to saveToDisk() {
					} handle IOError error {
						error
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		val variable = lintResult.find<VariableValue> { variableValue -> variableValue.name == "error" }
		assertNotNull(variable?.type)
	}
}