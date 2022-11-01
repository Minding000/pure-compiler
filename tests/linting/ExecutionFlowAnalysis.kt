package linting

import components.linting.semantic_model.types.ObjectType
import components.linting.semantic_model.values.VariableValue
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertIs

class ExecutionFlowAnalysis {

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
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Statement is unreachable.")
	}

	@Test
	fun `considers branches that will be executed`() {
		val sourceCode =
			"""
				loop {
					val shouldBreak = yes
					if(shouldBreak)
						break
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Statement is unreachable.")
	}

	@Test
	fun `ignores branches that wont be executed`() {
		val sourceCode =
			"""
				loop {
					if(no)
						break
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Statement is unreachable.")
	}

	@Test
	fun `calculates result of trivial conditional casts`() {
		val sourceCode =
			"""
				enum Color {
					instances RED
				}
				loop {
					if(Color.RED is Color)
						return
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Statement is unreachable.")
	}

	@Test
	fun `calculates result of trivial negated conditional casts`() {
		val sourceCode =
			"""
				enum Color {
					instances RED
				}
				val bird = null
				loop {
					if(bird is! Color)
						return
					next
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Statement is unreachable.")
	}

	@Test
	fun `calculates result of trivial optional casts`() {
		val sourceCode =
			"""
				enum Color {
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
