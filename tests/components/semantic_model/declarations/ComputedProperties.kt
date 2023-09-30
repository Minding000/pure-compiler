package components.semantic_model.declarations

import logger.Severity
import logger.issues.declaration.ComputedPropertyMissingType
import logger.issues.declaration.ComputedVariableWithoutSetter
import logger.issues.declaration.SetterInComputedValue
import logger.issues.initialization.ConstantReassignment
import logger.issues.modifiers.OverridingMemberKindMismatch
import logger.issues.modifiers.OverridingPropertyTypeMismatch
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil

internal class ComputedProperties {

	//TODO what about multiline getters and setters?
	// -> require type, because inference can be ambiguous
	@Disabled
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

	@Test
	fun `allows computed properties to overwrite computed properties`() {
		val sourceCode = """
			Computer class {
				val result: Int gets 0
			}
			ClassicalComputer class: Computer {
				overriding val result: Int gets 1
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OverridingPropertyTypeMismatch>()
	}

	@Test
	fun `disallows computed properties to overwrite properties`() {
		val sourceCode = """
			Computer class {
				val result = 0
			}
			ClassicalComputer class: Computer {
				overriding val result: Int gets 1
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingMemberKindMismatch>(
			"'result' computed property cannot override 'result' property.", Severity.ERROR)
	}

	@Test
	fun `disallows computed properties to overwrite functions`() {
		val sourceCode = """
			Computer class {
				to result()
			}
			ClassicalComputer class: Computer {
				overriding val result: Int gets 1
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingMemberKindMismatch>(
			"'result' computed property cannot override 'result' function.", Severity.ERROR)
	}

	//TODO use 'computed' keyword instead of 'val' and 'var' for computed properties

	//TODO write test: allows overriding computed properties to add getter / setter
}
