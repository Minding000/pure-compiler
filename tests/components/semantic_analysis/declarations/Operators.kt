package components.semantic_analysis.declarations

import logger.Severity
import logger.issues.definition.InvalidVariadicParameterPosition
import logger.issues.definition.MultipleVariadicParameters
import logger.issues.definition.Redeclaration
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Operators {

	@Test
	fun `allows operator declarations`() {
		val sourceCode =
			"""
				Time class
				Mood class
				Human class {
					operator [time: Time]: Mood
					operator [start: Time, end: Time]: Mood
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<Redeclaration>()
	}

	@Test
	fun `detects operator redeclarations`() {
		val sourceCode =
			"""
				Time class
				alias T = Time
				Human class {
					operator [start: T, end: T](time: T)
					operator [time: T]: T
					operator [time: Time]: Time
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<Redeclaration>(
			"Redeclaration of operator 'Human[Time]: Time', previously declared in Test.Test:5:10.", Severity.ERROR)
	}

	@Test
	fun `allows single variadic parameter`() {
		val sourceCode =
			"""
				Window class
				House class {
					operator +=(windows: ...Window)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MultipleVariadicParameters>()
		lintResult.assertIssueNotDetected<InvalidVariadicParameterPosition>()
	}

	@Test
	fun `detects multiple variadic parameters`() {
		val sourceCode =
			"""
				Window class
				House class {
					operator +=(openWindows: ...Window, closedWindows: ...Window)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MultipleVariadicParameters>("Signatures can have at most one variadic parameter.",
			Severity.ERROR)
	}

	@Test
	fun `detects variadic parameters not positioned at the parameter list end`() {
		val sourceCode =
			"""
				Window class
				House class {
					operator +=(windows: ...Window, selectedWindow: Window)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<InvalidVariadicParameterPosition>("Variadic parameters have to be the last parameter.",
			Severity.ERROR)
	}
}
