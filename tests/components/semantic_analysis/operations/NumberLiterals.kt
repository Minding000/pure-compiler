package components.semantic_analysis.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.values.VariableValue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertTrue

internal class NumberLiterals {

	@Test
	fun `assigns integer type to whole number`() {
		val sourceCode =
			"""
				val number = 234324
				number
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableType = lintResult.find<VariableValue>()?.type
		assertTrue(Linter.LiteralType.INTEGER.matches(variableType))
	}

	@Test
	fun `assigns integer type to zero without trailing zeros`() {
		val sourceCode =
			"""
				val number = 0
				number
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableType = lintResult.find<VariableValue>()?.type
		assertTrue(Linter.LiteralType.INTEGER.matches(variableType))
	}

	@Disabled
	@Test
	fun `assigns integer type to negative whole number`() {
		val sourceCode =
			"""
				val number = -6876
				number
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableType = lintResult.find<VariableValue>()?.type
		assertTrue(Linter.LiteralType.INTEGER.matches(variableType))
	}

	@Test
	fun `assigns float type to decimal number`() {
		val sourceCode =
			"""
				val number = 6534.75456
				number
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableType = lintResult.find<VariableValue>()?.type
		assertTrue(Linter.LiteralType.FLOAT.matches(variableType))
	}

	@Test
	fun `assigns float type to zero with trailing zeros`() {
		val sourceCode =
			"""
				val number = 0.00
				number
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableType = lintResult.find<VariableValue>()?.type
		assertTrue(Linter.LiteralType.FLOAT.matches(variableType))
	}

	@Disabled
	@Test
	fun `assigns float type to negative decimal number`() {
		val sourceCode =
			"""
				val number = -67.96
				number
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableType = lintResult.find<VariableValue>()?.type
		assertTrue(Linter.LiteralType.FLOAT.matches(variableType))
	}
}
