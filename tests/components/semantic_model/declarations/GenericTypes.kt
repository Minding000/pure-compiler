package components.semantic_model.declarations

import components.semantic_model.values.VariableValue
import logger.Severity
import logger.issues.constant_conditions.TypeNotAssignable
import logger.issues.declaration.GenericTypeDeclarationInObject
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
		lintResult.assertIssueNotDetected<GenericTypeDeclarationInObject>()
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
		lintResult.assertIssueNotDetected<GenericTypeDeclarationInObject>()
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
		lintResult.assertIssueDetected<GenericTypeDeclarationInObject>("Generic type declarations are not allowed in objects.",
			Severity.WARNING)
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

	@Test
	fun `doesn't check specific copies for issues`() {
		val sourceCode =
			"""
				Int class
				List class {
					containing Element

					val firstElement = getFirstElement()

					to getFirstElement(): Element
				}
				val integerList = <Int>List()
				integerList.firstElement
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeNotAssignable>()
	}

	@Test
	fun `implicit types in specific copies are available`() {
		val sourceCode =
			"""
				val integerList = <Int>List()
				integerList.firstElement
				Int class
				List class {
					containing Element

					val firstElement = getFirstElement()

					to getFirstElement(): Element
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableValueType = lintResult.find<VariableValue> { variableValue -> variableValue.name == "firstElement" }?.providedType
		assertEquals("Int", variableValueType.toString())
	}
}
