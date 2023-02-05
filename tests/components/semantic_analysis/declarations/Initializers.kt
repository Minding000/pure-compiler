package components.semantic_analysis.declarations

import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull

internal class Initializers {

	@Test
	fun `allows initializers in classes`() {
		val sourceCode =
			"""
				Human class {
					init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Initializers are only allowed inside")
	}

	@Test
	fun `allows initializers in enums`() {
		val sourceCode =
			"""
				Mood enum {
					init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Initializers are only allowed inside")
	}

	@Test
	fun `allows initializers inside objects without parameters`() {
		val sourceCode =
			"""
				Root object {
					init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "can not take parameters")
	}

	@Test
	fun `detects initializers inside objects with type parameters`() {
		val sourceCode =
			"""
				Root object {
					init(ChildType;)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Object initializers can not take type parameters")
	}

	@Test
	fun `detects initializers inside objects with parameters`() {
		val sourceCode =
			"""
				Root object {
					init(childCount: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Object initializers can not take parameters")
	}

	@Test
	fun `detects redeclarations of initializer signatures`() {
		val sourceCode =
			"""
				Trait class
				alias T = Trait
				Human class {
					init
					init(t: T)
					init(t: Trait)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Redeclaration of initializer 'Human(Trait)'")
	}

	@Test
	fun `creates default initializer if no initializer is defined`() {
		val sourceCode =
			"""
				Human class
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val defaultInitializer = lintResult.find<InitializerDefinition>()
		assertNotNull(defaultInitializer)
	}
}
