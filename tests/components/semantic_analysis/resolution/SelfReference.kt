package components.semantic_analysis.resolution

import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.values.SelfReference
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class SelfReference {

	@Test
	fun `detects self reference keyword outside of type definition`() {
		val sourceCode =
			"""
				this
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"Self references are not allowed outside of type definitions")
	}

	@Test
	fun `resolves self reference keyword in class`() {
		val sourceCode =
			"""
				Car class {
					to getCar(): Car {
						return this
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val selfReferenceType = lintResult.find<SelfReference>()?.type
		assertIs<ObjectType>(selfReferenceType)
		assertEquals("Car", selfReferenceType.definition?.name)
	}

	@Test
	fun `resolves self reference keyword in enum`() {
		val sourceCode =
			"""
				Car enum {
					to getCar(): Car {
						return this
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val selfReferenceType = lintResult.find<SelfReference>()?.type
		assertIs<ObjectType>(selfReferenceType)
		assertEquals("Car", selfReferenceType.definition?.name)
	}

	@Test
	fun `resolves self reference keyword in object`() {
		val sourceCode =
			"""
				FastestCar object {
					to getCar(): FastestCar {
						return this
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val selfReferenceType = lintResult.find<SelfReference>()?.type
		assertIs<ObjectType>(selfReferenceType)
		assertEquals("FastestCar", selfReferenceType.definition?.name)
	}
}
