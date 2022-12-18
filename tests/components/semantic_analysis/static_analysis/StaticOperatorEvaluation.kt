package components.semantic_analysis.static_analysis

import components.semantic_analysis.semantic_model.operations.BinaryOperator
import components.semantic_analysis.semantic_model.operations.UnaryOperator
import components.semantic_analysis.semantic_model.values.BooleanLiteral
import components.semantic_analysis.semantic_model.values.NumberLiteral
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class StaticOperatorEvaluation {

	@Test
	fun `calculates result of boolean negations`() {
		val sourceCode =
			"""
				!yes
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<UnaryOperator>()?.staticValue
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
		val staticResult = lintResult.find<UnaryOperator>()?.staticValue
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
		val staticResult = lintResult.find<BinaryOperator>()?.staticValue
		assertIs<NumberLiteral>(staticResult)
		assertEquals(22, staticResult.value.toInt())
	}

	@Test
	fun `calculates result of boolean and`() {
		val sourceCode =
			"""
				yes & no
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.staticValue
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(false, staticResult.value)
	}

	@Test
	fun `calculates result of boolean or`() {
		val sourceCode =
			"""
				yes | no
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.staticValue
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
		val staticResult = lintResult.find<BinaryOperator>()?.staticValue
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
		val staticResult = lintResult.find<BinaryOperator>()?.staticValue
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
		val staticResult = lintResult.find<BinaryOperator>()?.staticValue
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
		val staticResult = lintResult.find<BinaryOperator>()?.staticValue
		assertIs<NumberLiteral>(staticResult)
		assertEquals(32, staticResult.value.toInt())
	}

	@Test
	fun `calculates result of smaller than`() {
		val sourceCode =
			"""
				656 < 98
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.staticValue
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
		val staticResult = lintResult.find<BinaryOperator>()?.staticValue
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
		val staticResult = lintResult.find<BinaryOperator>()?.staticValue
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
		val staticResult = lintResult.find<BinaryOperator>()?.staticValue
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(true, staticResult.value)
	}

	@Test
	fun `calculates result of equals`() {
		val sourceCode =
			"""
				no == no
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.staticValue
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(true, staticResult.value)
	}

	@Test
	fun `calculates result of not equals`() {
		val sourceCode =
			"""
				98 != 554
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val staticResult = lintResult.find<BinaryOperator>()?.staticValue
		assertIs<BooleanLiteral>(staticResult)
		assertEquals(true, staticResult.value)
	}
}
