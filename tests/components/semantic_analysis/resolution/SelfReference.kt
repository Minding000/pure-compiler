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
	fun `allows self reference keyword inside of type definition`() {
		val sourceCode =
			"""
				Car class {
					to test() {
						this
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Self references are not allowed outside of type definitions")
	}

	@Test
	fun `disallows self reference keyword outside of type definition`() {
		val sourceCode =
			"""
				this
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Self references are not allowed outside of type definitions")
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

	@Test
	fun `references innermost class when there is no specifier`() {
		val sourceCode =
			"""
				Outer class {
					Inner class {
						to referenceInner() {
							this
						}
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val selfReference = lintResult.find<SelfReference>()
		assertEquals("Inner", selfReference?.type.toString())
	}

	@Test
	fun `references specified class when there is a specifier`() {
		val sourceCode =
			"""
				Outer class {
					Inner class {
						to referenceOuter() {
							this<Outer>
						}
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val selfReference = lintResult.find<SelfReference>()
		assertEquals("Outer", selfReference?.type.toString())
	}

	@Test
	fun `allows specifying a class that surrounds the reference`() {
		val sourceCode =
			"""
				Main class {
					to referenceMain() {
						this<Main>
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "Self references can only specify types they are bound to")
	}

	@Test
	fun `disallows specifying a class that it's not bound to`() {
		val sourceCode =
			"""
				Wrapper class {
					Main class {
						to referenceOther() {
							this<Wrapper>
						}
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Self references can only specify types they are bound to")
	}
}
