package components.semantic_analysis.resolution

import components.semantic_analysis.semantic_model.operations.MemberAccess
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class TypeResolution {

	@Test
	fun `emits error for undeclared types`() {
		val sourceCode =
			"""
				var eagle: Eagle
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Type 'Eagle' hasn't been declared yet")
	}

	@Test
	fun `resolves class types`() {
		val sourceCode =
			"""
				Bird class
				var bird: Bird
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<ValueDeclaration> { declaration -> declaration.name == "bird" }
		assertNotNull((declaration?.type as? ObjectType)?.definition)
	}

	@Test
	fun `resolves object types`() {
		val sourceCode =
			"""
				BirdFeeder object
				var birdFeeder: BirdFeeder
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<ValueDeclaration> { declaration -> declaration.name == "birdFeeder" }
		assertEquals("BirdFeeder", (declaration?.type as? ObjectType)?.definition?.name)
	}

	@Test
	fun `resolves enum types`() {
		val sourceCode =
			"""
				BirdType enum
				var birdType: BirdType
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<ValueDeclaration> { declaration -> declaration.name == "birdType" }
		assertEquals("BirdType", (declaration?.type as? ObjectType)?.definition?.name)
	}

	@Test
	fun `resolves type alias types`() {
		val sourceCode =
			"""
				alias EventHandler = =>|
				var eventHandler: EventHandler
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<ValueDeclaration> { declaration -> declaration.name == "eventHandler" }
		assertEquals("EventHandler", (declaration?.type as? ObjectType)?.definition?.name)
	}

	@Test
	fun `resolves enclosed types`() {
		val sourceCode =
			"""
				Bird class {
					Type enum
				}
				var birdType: Bird.Type
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<ValueDeclaration> { declaration -> declaration.name == "birdType" }
		assertEquals("Type", (declaration?.type as? ObjectType)?.definition?.name)
	}

	@Test
	fun `resolves generic types in definitions`() {
		val sourceCode =
			"""
				List class {
					containing Element
					to add(element: Element)
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<ValueDeclaration> { declaration -> declaration.name == "element" }
		assertEquals("Element", (declaration?.type as? ObjectType)?.definition?.name)
	}

	@Test
	fun `resolves generic types in function parameters`() {
		val sourceCode =
			"""
				List class {
					containing Element
					to add(element: Element) {}
				}
				Country class
				Germany object: Country {}
				val list = <Country>List()
				list.add(Germany)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val memberAccess = lintResult.find<MemberAccess> { memberAccess -> memberAccess.member.toString() == "add" }
		assertEquals("(Country) =>|", memberAccess?.type.toString())
	}
}
