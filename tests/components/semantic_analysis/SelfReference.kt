package components.semantic_analysis

import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.values.SelfReference
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class SelfReference {

	@Test
	fun `resolves self reference keyword in class`() {
		val sourceCode =
			"""
				class Car {
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
				object FastestCar {
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

	@Test
	fun `resolves self reference keyword in enum`() {
		val sourceCode =
			"""
				enum Car {
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
}
