package components.semantic_analysis.declarations

import messages.Message
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
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Computed properties need to have an explicitly declared type")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Computed value property cannot have a setter")
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Computed variable property needs to have a setter")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Computed value property cannot have a setter")
		lintResult.assertMessageNotEmitted(Message.Type.ERROR,
			"Computed variable property needs to have a setter")
	}

	@Test
	fun `disallows computed property without setter to be declared as 'var'`() {
		val sourceCode = """
			Computer object {
				var deviceCount: Int
				val monitorCount: Int
					gets deviceCount
					sets deviceCount = monitorCount
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Computed value property cannot have a setter")
	}

	@Test
	fun `disallows computed property with setter to be declared as 'val'`() {
		val sourceCode = """
			Computer object {
				var deviceCount: Int
				var monitorCount: Int
					gets deviceCount
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Computed variable property needs to have a setter")
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
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "cannot be reassigned")
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
		lintResult.assertMessageEmitted( Message.Type.ERROR,
			"'powerSupplyCount' cannot be reassigned, because it is constant")
	}
}
