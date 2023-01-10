package components.semantic_analysis.declarations

import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class GenericTypes {

	@Test
	fun `allows generic types in classes`() {
		val sourceCode =
			"""
				Human class {
					containing JobType
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING,
			"Generic type declarations are not allowed")
	}

	@Test
	fun `disallows generic types in enums`() {
		val sourceCode =
			"""
				Mood enum {
					containing JobType
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING,
			"Generic type declarations are not allowed")
	}

	@Test
	fun `emits warning for generic types in objects`() {
		val sourceCode =
			"""
				Earth object {
					containing Species
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"Generic type declarations are not allowed in objects")
	}

	@Test
	fun `resolves generic parameters in super type`() {
		val sourceCode =
			"""
				Int class
				List class {
					containing Element

					to getFirst(): Element
				}
				IntegerList class: <Int>List {

					init {
						val firstElement = getFirst()
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableType = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "firstElement" }?.type
		assertEquals("Int", variableType.toString())
	}
}
