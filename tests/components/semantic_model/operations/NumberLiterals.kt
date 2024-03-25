package components.semantic_model.operations

import components.semantic_model.context.SpecialType
import components.semantic_model.values.NumberLiteral
import logger.issues.resolution.SignatureAmbiguity
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
		val literalType = lintResult.find<NumberLiteral>()?.providedType
		assertTrue(SpecialType.INTEGER.matches(literalType), "Unexpected type '$literalType'")
	}

	@Test
	fun `assigns integer type to zero without trailing zeros`() {
		val sourceCode =
			"""
				0
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val literalType = lintResult.find<NumberLiteral>()?.providedType
		assertTrue(SpecialType.INTEGER.matches(literalType), "Unexpected type '$literalType'")
	}

	@Test
	fun `assigns float type to decimal number`() {
		val sourceCode =
			"""
				6534.75456
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val literalType = lintResult.find<NumberLiteral>()?.providedType
		assertTrue(SpecialType.FLOAT.matches(literalType), "Unexpected type '$literalType'")
	}

	@Test
	fun `assigns float type to zero with trailing zeros`() {
		val sourceCode =
			"""
				0.00
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val literalType = lintResult.find<NumberLiteral>()?.providedType
		assertTrue(SpecialType.FLOAT.matches(literalType), "Unexpected type '$literalType'")
	}

	@Test
	fun `assigns byte type to whole number in byte expression`() {
		val sourceCode =
			"""
				referencing Pure
				val number: Byte = 54
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val literalType = lintResult.find<NumberLiteral>()?.providedType
		assertTrue(SpecialType.BYTE.matches(literalType), "Unexpected type '$literalType'")
	}

	@Test
	fun `assigns byte type to negative whole number in byte expression`() {
		val sourceCode =
			"""
				referencing Pure
				val number: Byte = -54
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val literalType = lintResult.find<NumberLiteral>()?.providedType
		assertTrue(SpecialType.BYTE.matches(literalType), "Unexpected type '$literalType'")
	}

	@Test
	fun `assigns float type to whole number in float expression`() {
		val sourceCode =
			"""
				referencing Pure
				val number: Float = 54
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val literalType = lintResult.find<NumberLiteral>()?.providedType
		assertTrue(SpecialType.FLOAT.matches(literalType), "Unexpected type '$literalType'")
	}

	@Test
	fun `assigns float type to negative whole number in float expression`() {
		val sourceCode =
			"""
				referencing Pure
				val number: Float = -54
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val literalType = lintResult.find<NumberLiteral>()?.providedType
		assertTrue(SpecialType.FLOAT.matches(literalType), "Unexpected type '$literalType'")
	}

	@Test
	fun `assigns correct value to zero`() {
		val sourceCode =
			"""
				0
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<NumberLiteral>()?.getComputedValue()
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
		val variableValue = lintResult.find<NumberLiteral>()?.getComputedValue()
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
		val variableValue = lintResult.find<NumberLiteral>()?.getComputedValue()
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
		val variableValue = lintResult.find<NumberLiteral>()?.getComputedValue()
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
		val variableValue = lintResult.find<NumberLiteral>()?.getComputedValue()
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
		val variableValue = lintResult.find<NumberLiteral>()?.getComputedValue()
		assertIs<NumberLiteral>(variableValue)
		assertEquals(BigDecimal("1.2e7"), variableValue.value)
	}

	@Test
	fun `number literals default to the smallest type when call is ambiguous`() {
		val sourceCode =
			"""
				referencing Pure
				val typedNumber: Int = 2
				typedNumber + 1
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueNotDetected<SignatureAmbiguity>()
	}
}
