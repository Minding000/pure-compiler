package components.semantic_analysis.declarations

import logger.Severity
import logger.issues.definition.InvalidVariadicParameterPosition
import logger.issues.definition.MultipleVariadicParameters
import logger.issues.definition.Redeclaration
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Functions {

	@Test
	fun `allows function declarations`() {
		val sourceCode =
			"""
				Int class
				Human class {
					to push()
					to push(pressure: Int)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<Redeclaration>()
	}

	@Test
	fun `detects function redeclarations`() {
		val sourceCode =
			"""
				Pressure class
				alias P = Pressure
				Human class {
					to push(): Pressure
					to push(pressure: P)
					to push(pressure: Pressure)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<Redeclaration>(
			"Redeclaration of function 'Human.push(Pressure)', previously declared in Test.Test:5:4.", Severity.ERROR)
	}

	@Test
	fun `allows single variadic parameter`() {
		val sourceCode =
			"""
				Window class
				House object {
					to add(...windows: ...Window)
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
				House object {
					to add(...openWindows: ...Window, ...closedWindows: ...Window)
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
				House object {
					to add(...windows: ...Window, selectedWindow: Window)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<InvalidVariadicParameterPosition>("Variadic parameters have to be the last parameter.",
			Severity.ERROR)
	}
}
