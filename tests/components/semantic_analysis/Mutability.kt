package components.semantic_analysis

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Mutability {

	@Disabled
	@Test
	fun `prohibits mutation of immutable variable`() {
		val sourceCode =
			"""
				Bottle class {
					var contentInLitres: Float
					mutating to empty() {
						contentInLitres = 0
					}
				}
				immutable val bottleOfWater = Bottle()
				bottleOfWater.empty()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		//lintResult.assertIssueDetected(Message.Type.ERROR, "Cannot mutate immutable variable 'bottleOfWater'.")
	}
}
