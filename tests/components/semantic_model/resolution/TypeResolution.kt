package components.semantic_model.resolution

import components.semantic_model.declarations.PropertyDeclaration
import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.operations.MemberAccess
import components.semantic_model.types.ObjectType
import logger.issues.declaration.TypeParameterCountMismatch
import logger.issues.resolution.NotFound
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class TypeResolution {

	@Test
	fun `emits error for undeclared types`() {
		val sourceCode =
			"""
				var eagle: Eagle
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<NotFound>("Type 'Eagle' hasn't been declared yet.")
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
		assertNotNull((declaration?.providedType as? ObjectType)?.getTypeDeclaration())
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
		assertEquals("BirdFeeder", (declaration?.providedType as? ObjectType)?.getTypeDeclaration()?.name)
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
		assertEquals("BirdType", (declaration?.providedType as? ObjectType)?.getTypeDeclaration()?.name)
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
		assertEquals("EventHandler", (declaration?.providedType as? ObjectType)?.getTypeDeclaration()?.name)
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
		assertEquals("Type", (declaration?.providedType as? ObjectType)?.getTypeDeclaration()?.name)
	}

	@Test
	fun `doesn't resolve types enclosed by enclosing type of super type`() {
		val sourceCode =
			"""
				Editor class {
					Text class
					Input class
				}
				AccessibleInput class: Editor.Input {
					var value: Text
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<ValueDeclaration> { declaration -> declaration.name == "value" }
		assertNotNull(declaration)
		val type = declaration.providedType
		assertNotNull(type)
		assertIs<ObjectType>(type)
		assertNull(type.getTypeDeclaration())
	}

	@Test
	fun `resolves types enclosed in generic types`() {
		val sourceCode =
			"""
				List class {
					containing Element
					View class
				}
				var listView: List.View
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<TypeParameterCountMismatch>()
		val declaration = lintResult.find<ValueDeclaration> { declaration -> declaration.name == "listView" }
		assertEquals("View", (declaration?.providedType as? ObjectType)?.getTypeDeclaration()?.name)
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
		assertEquals("Element", (declaration?.providedType as? ObjectType)?.getTypeDeclaration()?.name)
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
				Germany object: Country
				val list = <Country>List()
				list.add(Germany)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val memberAccess = lintResult.find<MemberAccess> { memberAccess -> memberAccess.member.toString() == "add" }
		assertEquals("(Country) =>|", memberAccess?.providedType.toString())
	}

	@Test
	fun `resolves implicit type of self referencing property`() {
		val sourceCode =
			"""
				List class {
					containing Element
					val interface = this
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val propertyDeclaration = lintResult.find<PropertyDeclaration> { propertyDeclaration -> propertyDeclaration.name == "interface" }
		assertEquals("Self", propertyDeclaration?.providedType.toString())
	}

	@Test
	fun `resolves implicit type of property referencing following property`() {
		val sourceCode =
			"""
				List class {
					val size = this.length
					val length: Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val propertyDeclaration = lintResult.find<PropertyDeclaration> { propertyDeclaration -> propertyDeclaration.name == "size" }
		assertEquals("Int", propertyDeclaration?.providedType.toString())
	}
}
