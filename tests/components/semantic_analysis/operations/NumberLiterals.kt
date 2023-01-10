package components.semantic_analysis.operations

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.values.NumberLiteral
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class NumberLiterals {

	@Test
	fun `assigns integer type to whole number`() {
		val sourceCode =
			"""
				234324
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableType = lintResult.find<NumberLiteral>()?.type
		assertTrue(Linter.SpecialType.INTEGER.matches(variableType))
	}

	@Test
	fun `assigns integer type to zero without trailing zeros`() {
		val sourceCode =
			"""
				0
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableType = lintResult.find<NumberLiteral>()?.type
		assertTrue(Linter.SpecialType.INTEGER.matches(variableType))
	}

	@Disabled
	@Test
	fun `assigns integer type to negative whole number`() {
		val sourceCode =
			"""
				-6876
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableType = lintResult.find<NumberLiteral>()?.type
		assertTrue(Linter.SpecialType.INTEGER.matches(variableType))
	}

	@Test
	fun `assigns float type to decimal number`() {
		val sourceCode =
			"""
				6534.75456
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableType = lintResult.find<NumberLiteral>()?.type
		assertTrue(Linter.SpecialType.FLOAT.matches(variableType))
	}

	@Test
	fun `assigns float type to zero with trailing zeros`() {
		val sourceCode =
			"""
				0.00
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableType = lintResult.find<NumberLiteral>()?.type
		assertTrue(Linter.SpecialType.FLOAT.matches(variableType))
	}

	@Disabled
	@Test
	fun `assigns float type to negative decimal number`() {
		val sourceCode =
			"""
				-67.96
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableType = lintResult.find<NumberLiteral>()?.type
		assertTrue(Linter.SpecialType.FLOAT.matches(variableType))
	}

	@Test
	fun `assigns float type to whole number in integer expression`() {
		val sourceCode =
			"""
				referencing Pure
				val number: Float = 54
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val variableType = lintResult.find<NumberLiteral>()?.type
		assertTrue(Linter.SpecialType.FLOAT.matches(variableType))
	}

	@Test
	fun `assigns correct value to zero`() {
		val sourceCode =
			"""
				0
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<NumberLiteral>()?.staticValue
		assertIs<NumberLiteral>(variableValue)
		assertEquals(BigDecimal("0"), variableValue.value)
	}

	@Test
	fun `assigns correct value to integer number`() {
		val sourceCode =
			"""
				14
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<NumberLiteral>()?.staticValue
		assertIs<NumberLiteral>(variableValue)
		assertEquals(BigDecimal("14"), variableValue.value)
	}

	@Test
	fun `assigns correct value to decimal number`() {
		val sourceCode =
			"""
				6.546
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<NumberLiteral>()?.staticValue
		assertIs<NumberLiteral>(variableValue)
		assertEquals(BigDecimal("6.546"), variableValue.value)
	}

	@Test
	fun `assigns correct value to number using thousands separator before the decimal point`() {
		val sourceCode =
			"""
				1_000_000
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<NumberLiteral>()?.staticValue
		assertIs<NumberLiteral>(variableValue)
		assertEquals(BigDecimal("1000000"), variableValue.value)
	}

	@Test
	fun `assigns correct value to number using thousands separator after the decimal point`() {
		val sourceCode =
			"""
				0.00_000_1
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<NumberLiteral>()?.staticValue
		assertIs<NumberLiteral>(variableValue)
		assertEquals(BigDecimal("0.000001"), variableValue.value)
	}

	@Test
	fun `assigns correct value to number using scientific notation`() {
		val sourceCode =
			"""
				1.2e7
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<NumberLiteral>()?.staticValue
		assertIs<NumberLiteral>(variableValue)
		assertEquals(BigDecimal("1.2e7"), variableValue.value)
	}
}
