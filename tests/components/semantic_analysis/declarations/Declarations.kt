package components.semantic_analysis.declarations

import components.semantic_analysis.semantic_model.values.VariableValue
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull

internal class Declarations {

	@Test
	fun `emits error for incompatible source expression type`() {
		val sourceCode =
			"""
				Toast class {}
				Banana object {}
				var toast: Toast = Banana
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Type 'Banana' is not assignable to type 'Toast'")
	}

	@Test
	fun `emits error if no type is provided to variable declaration`() {
		val sourceCode =
			"""
				var toast
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Type or value is required")
	}

	@Test
	fun `detects shadowed variables`() {
		val sourceCode =
			"""
				Handler class {}
				val defaultHandler: Handler
				Event class {
					const defaultHandler: Handler
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "'defaultHandler' shadows a member")
	}

	@Test
	fun `detects redeclarations of variables`() {
		val sourceCode =
			"""
				Car class {}
				var car: Car
				val car: Car
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Redeclaration of value 'car'")
	}

	@Test
	fun `detects redeclarations of types`() {
		val sourceCode =
			"""
				Animal class {}
				Animal enum {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Redeclaration of type 'Animal'")
	}

	@Test
	fun `detects redeclarations of function signatures`() {
		val sourceCode =
			"""
				Pressure class {}
				alias P = Pressure
				Human class {
					to sit(): Pressure {}
					to sit(pressure: P) {}
					to sit(pressure: Pressure) {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Redeclaration of function 'sit(Pressure)'")
	}

	@Test
	fun `detects redeclarations of operator signatures`() {
		val sourceCode =
			"""
				Time class {}
				alias T = Time
				Human class {
					operator [start: T, end: T](time: T) {}
					operator [time: T]: T {}
					operator [time: Time]: Time {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Redeclaration of operator 'Human[Time](): Time'")
	}

	@Test
	fun `detects invalid modifiers`() {
		val sourceCode =
			"""
				overriding House class {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Modifier 'overriding' is not allowed here")
	}

	@Test
	fun `detects duplicate modifiers`() {
		val sourceCode =
			"""
				native native Memory class {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Duplicate 'native' modifier")
	}

	@Test
	fun `handle block declares error variable`() {
		val sourceCode =
			"""
				IOError class {}
				Config class {
					to saveToDisk() {
					} handle error: IOError {
						error
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variable = lintResult.find<VariableValue> { variableValue -> variableValue.name == "error" }
		assertNotNull(variable?.type)
	}

	@Test
	fun `emits warning for generic non-index operator`() {
		val sourceCode = """
			Vector class {
				operator +(ReturnType; other: Self): ReturnType
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"Operators (except for the index operator) can not be generic")
	}

	@Test
	fun `emits warning for generic parameters in parentheses in index operator`() {
		val sourceCode = """
			Vector class {
				operator [key: IndexType](IndexType; value: Int)
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"Generic parameters for the index operator are received in the index parameter list instead")
	}
}
