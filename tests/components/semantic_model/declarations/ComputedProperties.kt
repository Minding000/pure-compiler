package components.semantic_model.declarations

import logger.Severity
import logger.issues.declaration.ComputedPropertyMissingType
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
				computed monitorCount
					gets deviceCount
					sets deviceCount = monitorCount
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<ComputedPropertyMissingType>(
			"Computed properties need to have an explicitly declared type.", Severity.ERROR)
	}

	@Test
	fun `allows writes to computed property with setter`() {
		val sourceCode = """
			Computer object {
				var deviceCount: Int
				computed monitorCount: Int
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
				computed powerSupplyCount: Int
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
				computed result: Int gets 0
			}
			ClassicalComputer class: Computer {
				overriding computed result: Int gets 1
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
				overriding computed result: Int gets 1
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
				overriding computed result: Int gets 1
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingMemberKindMismatch>(
			"'result' computed property cannot override 'result' function.", Severity.ERROR)
	}

	//TODO write test: allows overriding computed properties to add getter / setter
}
