package components.semantic_analysis.operations

import logger.Severity
import logger.issues.definition.InvalidInstanceLocation
import logger.issues.definition.MultipleInstanceLists
import logger.issues.loops.BreakStatementOutsideOfLoop
import logger.issues.loops.NextStatementOutsideOfLoop
import logger.issues.switches.DuplicateCase
import logger.issues.switches.RedundantElse
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Statements {

	@Test
	fun `emits error for duplicate case in switch statement`() {
		val sourceCode =
			"""
				OperatingSystem enum {
					instances WINDOWS, LINUX, MACOS
				}
				val operatingSystem = OperatingSystem.WINDOWS
				switch operatingSystem {
					.WINDOWS:
						cli.printLine("Windows")
					.LINUX:
						cli.printLine("Linux")
					.WINDOWS:
						cli.printLine("MacOS")
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<DuplicateCase>("Duplicated case '.WINDOWS', previously defined in Test.Test:6:1.",
			Severity.WARNING)
	}

	@Test
	fun `emits warning for redundant else branches in switches`() {
		val sourceCode =
			"""
				switch yes {
					yes:
						cli.printLine("yes")
					no:
						cli.printLine("no")
					else:
						cli.printLine("else")
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<RedundantElse>(
			"The else branch is redundant, because the switch is already exhaustive without it.", Severity.WARNING)
	}

	@Test
	fun `doesn't emit warning for non-redundant else branches in switches`() {
		val sourceCode =
			"""
				switch yes {
					yes:
						cli.printLine("yes")
					else:
						cli.printLine("else")
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<RedundantElse>()
	}

	@Test
	fun `emits warning for instances on objects`() {
		val sourceCode =
			"""
				Date object {
					instances CURRENT
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<InvalidInstanceLocation>("Instance declarations are only allowed in enums and classes.",
			Severity.WARNING)
	}

	@Test
	fun `emits warning for multiple instances declarations`() {
		val sourceCode =
			"""
				Date enum {
					instances PAST
					instances CURRENT
					instances FUTURE
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MultipleInstanceLists>("Instance declarations can be merged.", Severity.WARNING, 3)
		lintResult.assertIssueDetected<MultipleInstanceLists>("Instance declarations can be merged.", Severity.WARNING, 4)
	}

	@Test
	fun `detects break statements outside of loops`() {
		val sourceCode =
			"""
				break
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<BreakStatementOutsideOfLoop>("Break statements are not allowed outside of loops.",
			Severity.ERROR)
	}

	@Test
	fun `ignores break statements inside of loops`() {
		val sourceCode =
			"""
				loop {
					break
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<BreakStatementOutsideOfLoop>()
	}

	@Test
	fun `detects next statements outside of loops`() {
		val sourceCode =
			"""
				next
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<NextStatementOutsideOfLoop>("Next statements are not allowed outside of loops.",
			Severity.ERROR)
	}

	@Test
	fun `ignores next statements inside of loops`() {
		val sourceCode =
			"""
				loop {
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<NextStatementOutsideOfLoop>()
	}
}
