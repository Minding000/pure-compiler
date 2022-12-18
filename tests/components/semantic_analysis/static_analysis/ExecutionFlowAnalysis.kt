package components.semantic_analysis.static_analysis

import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.values.VariableValue
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertIs

internal class ExecutionFlowAnalysis {

	@Test
	fun `emits warning for unreachable statements`() {
		val sourceCode =
			"""
				loop {
					break
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Statement is unreachable")
	}

	@Test
	fun `considers if branches that will be executed`() {
		val sourceCode =
			"""
				loop {
					val shouldBreak = yes
					if shouldBreak
						break
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Statement is unreachable")
	}

	@Test
	fun `ignores if branches that wont be executed`() {
		val sourceCode =
			"""
				loop {
					if no
						break
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Statement is unreachable")
	}

	@Test
	fun `considers switch branches that will be executed`() {
		val sourceCode =
			"""
				loop {
					val shouldBreak = yes
					switch shouldBreak {
						yes:
							break
					}
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Statement is unreachable")
	}

	@Test
	fun `ignores switch branches that wont be executed`() {
		val sourceCode =
			"""
				loop {
					val shouldBreak = no
					switch shouldBreak {
						yes:
							break
					}
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Statement is unreachable")
	}

	@Test
	fun `considers infinite loops that wont be broken`() {
		val sourceCode =
			"""
				loop {
					loop {
					}
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Statement is unreachable")
	}

	@Test
	fun `considers infinite while loops that wont be broken`() {
		val sourceCode =
			"""
				loop {
					loop while yes {
					}
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Statement is unreachable")
	}

	@Test
	fun `ignores finite loops`() {
		val sourceCode =
			"""
				loop {
					loop while no {
					}
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Statement is unreachable")
	}

	@Test
	fun `ignores infinite loops that may be broken`() {
		val sourceCode =
			"""
				loop {
					loop {
						break
					}
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Statement is unreachable")
	}

	@Test
	fun `calculates result of trivial conditional casts`() {
		val sourceCode =
			"""
				Color enum {
					instances RED
				}
				loop {
					if Color.RED is Color
						return
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Statement is unreachable")
	}

	@Test
	fun `calculates result of trivial negated conditional casts`() {
		val sourceCode =
			"""
				Color enum {
					instances RED
				}
				val bird = null
				loop {
					if bird is! Color
						return
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Statement is unreachable")
	}

	@Test
	fun `calculates result of trivial optional casts`() {
		val sourceCode =
			"""
				Color enum {
					instances RED
					init
				}
				loop {
					val result = Color.RED as? Color
					result
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "result" }
		assertIs<ObjectType>(variableValue?.staticValue?.type)
	}
}
