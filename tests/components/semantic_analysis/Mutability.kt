package components.semantic_analysis

import messages.Message
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Mutability {

	@Disabled
	@Test
	fun `prohibits mutation of immutable variable`() {
		val sourceCode =
			"""
				class Bottle {
					var contentInLitres: Float
					mutating to empty() {
						contentInLitres = 0
					}
				}
				immutable val bottleOfWater = Bottle()
				bottleOfWater.empty()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Cannot mutate immutable variable 'bottleOfWater'.")
	}
}
