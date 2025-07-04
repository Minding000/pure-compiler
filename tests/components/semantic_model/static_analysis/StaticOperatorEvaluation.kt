package components.semantic_model.static_analysis

import components.semantic_model.control_flow.IfExpression
import components.semantic_model.control_flow.SwitchExpression
import components.semantic_model.operations.BinaryOperator
import components.semantic_model.operations.UnaryOperator
import components.semantic_model.values.BooleanLiteral
import components.semantic_model.values.NumberLiteral
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

internal class StaticOperatorEvaluation {

	@Test
	fun `calculates result of boolean negations`() {
		val sourceCode =
			"""
				!yes
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<UnaryOperator>()?.getComputedValue()
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(false, staticResult.value)
	}

	@Test
	fun `calculates result of number inversions`() {
		val sourceCode =
			"""
				-768
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<UnaryOperator>()?.getComputedValue()
		assertIs<NumberLiteral>(staticResult)
		assertEquals(-768, staticResult.value.toInt())
	}

	@Test
	fun `calculates result of null coalescence`() {
		val sourceCode =
			"""
				null ?? 22
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<NumberLiteral>(staticResult)
		assertEquals(22, staticResult.value.toInt())
	}

	@Test
	fun `calculates result of boolean and`() {
		val sourceCode =
			"""
				yes and no
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(false, staticResult.value)
	}

	@Test
	fun `calculates result of boolean or`() {
		val sourceCode =
			"""
				yes or no
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(true, staticResult.value)
	}

	@Test
	fun `calculates result of integer addition`() {
		val sourceCode =
			"""
				1 + 1
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<NumberLiteral>(staticResult)
		assertEquals(2, staticResult.value.toInt())
	}

	@Test
	fun `calculates result of integer subtraction`() {
		val sourceCode =
			"""
				256 - 1
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<NumberLiteral>(staticResult)
		assertEquals(255, staticResult.value.toInt())
	}

	@Test
	fun `calculates result of integer multiplication`() {
		val sourceCode =
			"""
				4 * 8
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<NumberLiteral>(staticResult)
		assertEquals(32, staticResult.value.toInt())
	}

	@Test
	fun `calculates result of integer division`() {
		val sourceCode =
			"""
				128 / 4
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<NumberLiteral>(staticResult)
		assertEquals(32, staticResult.value.toInt())
	}

	@Test
	fun `ignores division by zero`() {
		val sourceCode =
			"""
				383 / 0
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertNull(staticResult)
	}

	@Test
	fun `calculates result of smaller than`() {
		val sourceCode =
			"""
				656 < 98
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(false, staticResult.value)
	}

	@Test
	fun `calculates result of greater than`() {
		val sourceCode =
			"""
				654 > 6
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(true, staticResult.value)
	}

	@Test
	fun `calculates result of smaller than or equal to`() {
		val sourceCode =
			"""
				13 <= 4657
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(true, staticResult.value)
	}

	@Test
	fun `calculates result of greater than or equal to`() {
		val sourceCode =
			"""
				87 >= 87
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(true, staticResult.value)
	}

	@Test
	fun `calculates result of identity comparison`() {
		val sourceCode =
			"""
				Int class
				val number = Int()
				number === number
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(true, staticResult.value)
	}

	@Test
	fun `calculates result of negated identity comparison`() {
		val sourceCode =
			"""
				Int class
				val number = Int()
				number !== number
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(false, staticResult.value)
	}

	@Test
	fun `calculates result of equality comparison`() {
		val sourceCode =
			"""
				no == no
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(true, staticResult.value)
	}

	@Test
	fun `calculates result of negated equality comparison`() {
		val sourceCode =
			"""
				98 != 554
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.getComputedValue()
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(true, staticResult.value)
	}

	@Test
	fun `calculates result of if expressions`() {
		val sourceCode =
			"""
				val x = if 2 == 3 50 else 45
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<IfExpression>()?.getComputedValue()
		assertIs<NumberLiteral>(staticResult)
		assertEquals(45, staticResult.value.toInt())
	}

	@Test
	fun `calculates result of switch expressions`() {
		val sourceCode =
			"""
				val x = switch 2 {
					3: 3
					else: 45
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<SwitchExpression>()?.getComputedValue()
		assertIs<NumberLiteral>(staticResult)
		assertEquals(45, staticResult.value.toInt())
	}
}
