package components.semantic_model.resolution

import components.semantic_model.types.SelfType
import components.semantic_model.values.SelfReference
import logger.Severity
import logger.issues.declaration.TypeParameterCountMismatch
import logger.issues.resolution.SelfReferenceOutsideOfTypeDefinition
import logger.issues.resolution.SelfReferenceSpecifierNotBound
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
		lintResult.assertIssueNotDetected<SelfReferenceOutsideOfTypeDefinition>()
	}

	@Test
	fun `disallows self reference keyword outside of type definition`() {
		val sourceCode =
			"""
				this
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<SelfReferenceOutsideOfTypeDefinition>(
			"Self references are not allowed outside of type definitions.", Severity.ERROR)
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
		val selfReferenceType = lintResult.find<SelfReference>()?.providedType
		assertIs<SelfType>(selfReferenceType)
		assertEquals("Car", selfReferenceType.typeDeclaration?.name)
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
		val selfReferenceType = lintResult.find<SelfReference>()?.providedType
		assertIs<SelfType>(selfReferenceType)
		assertEquals("Car", selfReferenceType.typeDeclaration?.name)
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
		val selfReferenceType = lintResult.find<SelfReference>()?.providedType
		assertIs<SelfType>(selfReferenceType)
		assertEquals("FastestCar", selfReferenceType.typeDeclaration?.name)
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
		val selfReferenceType = lintResult.find<SelfReference>()?.providedType
		assertIs<SelfType>(selfReferenceType)
		assertEquals("Inner", selfReferenceType.typeDeclaration?.name)
	}

	@Test
	fun `references specified class when there is a specifier`() {
		val sourceCode =
			"""
				Outer class {
					bound Inner class {
						to referenceOuter() {
							this<Outer>
						}
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val selfReferenceType = lintResult.find<SelfReference>()?.providedType
		assertIs<SelfType>(selfReferenceType)
		assertEquals("Outer", selfReferenceType.typeDeclaration?.name)
	}

	@Test
	fun `allows specifying enclosed type`() {
		val sourceCode =
			"""
				Outer class {
					bound Middle class
				}
				bound Inner class in Outer.Middle {
					to referenceMiddle() {
						this<Outer.Middle>
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val selfReferenceType = lintResult.find<SelfReference>()?.providedType
		assertIs<SelfType>(selfReferenceType)
		assertEquals("Middle", selfReferenceType.typeDeclaration?.name)
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
		lintResult.assertIssueNotDetected<SelfReferenceSpecifierNotBound>()
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
		lintResult.assertIssueDetected<SelfReferenceSpecifierNotBound>(
			"Specified type 'Wrapper' is not bound to type 'Main' surrounding the self reference.", Severity.ERROR)
	}

	@Test
	fun `specifier does not require generic parameters`() {
		val sourceCode =
			"""
				Wrapper class {
					containing Key
					val key: Key? = null
					bound Main class {
						var key: Key?
						to referenceOther() {
							key = this<Wrapper>.key
						}
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeParameterCountMismatch>()
	}
}
