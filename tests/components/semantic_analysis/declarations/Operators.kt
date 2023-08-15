package components.semantic_analysis.declarations

import components.semantic_analysis.semantic_model.declarations.FunctionImplementation
import logger.Severity
import logger.issues.declaration.Redeclaration
import logger.issues.declaration.VariadicParameterInOperator
import logger.issues.modifiers.OverridingFunctionReturnTypeNotAssignable
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

	@Test
	fun `detects link from operator to super operator with different return type`() {
		val sourceCode =
			"""
				Int class
				Float class
				House class {
					operator[a: Int]: Int
				}
				WoodenHouse class: House {
					overriding operator[a: Int]: Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val operator = lintResult.find<FunctionImplementation>(FunctionImplementation::isOverriding)
		assertNotNull(operator)
		assertNull(operator.signature.superFunctionSignature)
		lintResult.assertIssueDetected<OverridingFunctionReturnTypeNotAssignable>(
			"Return type of overriding operator 'WoodenHouse[Int]: Float' is not assignable to " +
				"the return type of the overridden operator 'House[Int]: Int'.", Severity.ERROR)
	}
}
