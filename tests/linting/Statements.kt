package linting

import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil

class Statements {

	@Test
	fun `emits error for duplicate case in switch statement`() {
		val sourceCode =
			"""
				enum OperatingSystem {
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
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Duplicated case '.WINDOWS'")
	}

	@Test
	fun `emits warning for instances on objects`() {
		val sourceCode =
			"""
				object Date {
					instances CURRENT
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"Instance declarations are only allowed in enums and classes")
	}

	@Test
	fun `emits warning for instances on traits`() {
		val sourceCode =
			"""
				trait Date {
					instances CURRENT
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"Instance declarations are only allowed in enums and classes")
	}

	@Test
	fun `emits warning for multiple instances declarations`() {
		val sourceCode =
			"""
				enum Date {
					instances PAST
					instances CURRENT
					instances FUTURE
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, false)
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"Instance declarations can be merged")
	}
}