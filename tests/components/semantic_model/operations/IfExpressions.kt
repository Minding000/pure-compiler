package components.semantic_model.operations

import components.semantic_model.declarations.LocalVariableDeclaration
import logger.Severity
import logger.issues.expressions.BranchMissesValue
import logger.issues.expressions.ExpressionMissesElse
import logger.issues.expressions.ExpressionNeverReturns
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class IfExpressions {

	@Test
	fun `allows if with else in expression`() {
		val sourceCode =
			"""
				val y = if yes 10 else 2
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ExpressionMissesElse>()
		lintResult.assertIssueNotDetected<BranchMissesValue>()
		lintResult.assertIssueNotDetected<ExpressionNeverReturns>()
	}

	@Test
	fun `detects if without else in expression`() {
		val sourceCode =
			"""
				val y = if yes 10
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ExpressionMissesElse>("The if expression is missing an else branch.", Severity.ERROR)
	}

	@Test
	fun `detects if branch without value in expression`() {
		val sourceCode =
			"""
				val y = 1
				y = if yes 10 else y = 2
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<BranchMissesValue>("This branch of the if expression is missing a value.", Severity.ERROR)
	}

	@Test
	fun `detects if expression that never returns`() {
		val sourceCode =
			"""
				val y = if yes return 2 else return 3
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ExpressionNeverReturns>("This if expression never returns a value.", Severity.ERROR)
	}

	@Test
	fun `if expressions return a value`() {
		val sourceCode =
			"""
				val y = if yes 10 else 2
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableDeclarationType = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "y" }?.type
		assertEquals("Int", variableDeclarationType.toString())
	}
}
