package components.semantic_analysis.declarations

import logger.Severity
import logger.issues.definition.Redeclaration
import logger.issues.definition.VariadicParameterInOperator
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
	fun `disallows variadic parameters in operators`() {
		val sourceCode =
			"""
				Window class
				House class {
					operator +=(...windows: ...Window)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<VariadicParameterInOperator>("Variadic parameter in operator definition.",
			Severity.ERROR)
	}
}
