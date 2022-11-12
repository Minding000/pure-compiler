package components.semantic_analysis

import components.semantic_analysis.semantic_model.values.VariableValue
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertNotNull

internal class ValueResolution {

	@Test
	fun `emits error for undeclared variables`() {
		val sourceCode =
			"""
				numberOfDogs
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Value 'numberOfDogs' hasn't been declared yet")
	}

	@Test
	fun `resolves local variables`() {
		val sourceCode =
			"""
				val numberOfCats = 2
				numberOfCats
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "numberOfCats" }
		assertNotNull(variableValue?.definition)
	}

	@Test
	fun `resolves instance members`() {
		val sourceCode =
			"""
				object House {
					val livingAreaInSquareMeters = 120
				}
				House.livingAreaInSquareMeters
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "livingAreaInSquareMeters" }
		assertNotNull(variableValue?.definition)
	}

	@Test
	fun `resolves parameters`() {
		val sourceCode =
			"""
				object House {
					to openDoor(speed: Int) {
						speed
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "speed" }
		assertNotNull(variableValue?.definition)
	}

	@Test
	fun `resolves super members`() {
		val sourceCode =
			"""
				class Door {
					val isOpen = yes
				}
				object GlassDoor: Door {
				}
				GlassDoor.isOpen
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val member = variableValue?.type?.scope?.resolveValue("isOpen")
		assertNotNull(member)
	}

	@Test
	fun `emits error for calls to uncallable value`() {
		val sourceCode =
			"""
				object Bird {
					var age = 0
				}
				Bird.age()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'Bird.age' is not callable")
	}
}
