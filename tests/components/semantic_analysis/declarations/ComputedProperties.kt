package components.semantic_analysis.declarations

import logger.Severity
import logger.issues.definition.ComputedPropertyMissingType
import logger.issues.definition.ComputedVariableWithoutSetter
import logger.issues.definition.SetterInComputedValue
import logger.issues.initialization.ConstantReassignment
import org.junit.jupiter.api.Test
import util.TestUtil

internal class ComputedProperties {

	@Test
	fun `requires type to be declared explicitly`() {
		val sourceCode = """
			Computer object {
				var deviceCount: Int
				var monitorCount
					gets deviceCount
					sets deviceCount = monitorCount
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ComputedPropertyMissingType>(
			"Computed properties need to have an explicitly declared type.", Severity.ERROR)
	}

	@Test
	fun `allows computed property with setter to be declared as 'var'`() {
		val sourceCode = """
			Computer object {
				var deviceCount: Int
				var monitorCount: Int
					gets deviceCount
					sets deviceCount = monitorCount
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<SetterInComputedValue>()
		lintResult.assertIssueNotDetected<ComputedVariableWithoutSetter>()
	}

	@Test
	fun `allows computed property without setter to be declared as 'val'`() {
		val sourceCode = """
			Computer object {
				var deviceCount: Int
				val monitorCount: Int
					gets deviceCount
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<SetterInComputedValue>()
		lintResult.assertIssueNotDetected<ComputedVariableWithoutSetter>()
	}

	@Test
	fun `disallows computed property without setter to be declared as 'var'`() {
		val sourceCode = """
			Computer object {
				var deviceCount: Int
				var monitorCount: Int
					gets deviceCount
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ComputedVariableWithoutSetter>("Computed variable property needs to have a setter.",
			Severity.ERROR)
	}

	@Test
	fun `disallows computed property with setter to be declared as 'val'`() {
		val sourceCode = """
			Computer object {
				var deviceCount: Int
				val monitorCount: Int
					gets deviceCount
					sets deviceCount = monitorCount
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<SetterInComputedValue>("Computed value property cannot have a setter.", Severity.ERROR)
	}

	@Test
	fun `allows writes to computed property with setter`() {
		val sourceCode = """
			Computer object {
				var deviceCount: Int
				var monitorCount: Int
					gets deviceCount
					sets deviceCount = monitorCount
			}
			Computer.monitorCount = 2
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<ConstantReassignment>()
	}

	@Test
	fun `disallows writes to computed property without setter`() {
		val sourceCode = """
			Computer object {
				val powerSupplyCount: Int
					gets 1
			}
			Computer.powerSupplyCount = 2
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ConstantReassignment>()
	}
}
