package components.semantic_analysis.operations

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Statements {

	@Test
	fun `emits error for duplicate case in switch statement`() {
		val sourceCode =
			"""
				OperatingSystem enum {
					instances WINDOWS, LINUX, MACOS
					init
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
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Duplicated case '.WINDOWS'")
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
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"The else branch is redundant, because the switch is already exhaustive without it")
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
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "else branch is redundant")
	}

	@Test
	fun `emits warning for instances on objects`() {
		val sourceCode =
			"""
				Date object {
					instances CURRENT
					init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"Instance declarations are only allowed in enums and classes")
	}

	@Test
	fun `emits warning for multiple instances declarations`() {
		val sourceCode =
			"""
				Date enum {
					instances PAST
					instances CURRENT
					instances FUTURE
					init
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Instance declarations can be merged")
	}

	@Test
	fun `detects break statements outside of loops`() {
		val sourceCode =
			"""
				break
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Break statements are not allowed outside of loops")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Break statements are not allowed outside of loops")
	}

	@Test
	fun `detects next statements outside of loops`() {
		val sourceCode =
			"""
				next
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Next statements are not allowed outside of loops")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Next statements are not allowed outside of loops")
	}
}
