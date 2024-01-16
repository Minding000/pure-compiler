package components.semantic_model.operations

import components.semantic_model.declarations.LocalVariableDeclaration
import logger.Severity
import logger.issues.expressions.BranchMissesValue
import logger.issues.expressions.ExpressionMissesElse
import logger.issues.expressions.ExpressionNeverReturns
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class SwitchExpressions {

	@Test
	fun `allows switch with else in expression`() {
		val sourceCode =
			"""
				val y = switch x {
					ExitCode.SUCCESS: "Success"
					else: "Failure"
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ExpressionMissesElse>()
		lintResult.assertIssueNotDetected<BranchMissesValue>()
		lintResult.assertIssueNotDetected<ExpressionNeverReturns>()
	}

	@Test
	fun `detects switch without else in expression`() {
		val sourceCode =
			"""
				val y = switch x {
					ExitCode.SUCCESS: "Success"
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ExpressionMissesElse>("The switch expression is missing an else branch.", Severity.ERROR)
	}

	@Test
	fun `detects switch branch without value in expression`() {
		val sourceCode =
			"""
				val y = 1
				val y = switch x {
					ExitCode.SUCCESS: 1
					else: y = 2
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<BranchMissesValue>("This branch of the switch expression is missing a value.", Severity.ERROR)
	}

	@Test
	fun `detects switch expression that never returns`() {
		val sourceCode =
			"""
				val y = switch x {
					ExitCode.SUCCESS: return "Success"
					else: return "Failure"
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ExpressionNeverReturns>("This switch expression never returns a value.", Severity.ERROR)
	}

	@Test
	fun `switch expressions return a value`() {
		val sourceCode =
			"""
				val y = switch x {
					ExitCode.SUCCESS: 1
					else: 2
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableDeclarationType = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "y" }?.type
		assertEquals("Int", variableDeclarationType.toString())
	}
}
