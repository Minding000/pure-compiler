package components.semantic_analysis

import components.semantic_analysis.semantic_model.operations.MemberAccess
import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.values.*
import util.TestUtil
import messages.Message
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

internal class TypeResolution {

	@Test
	fun `loads string literal type`() {
		val sourceCode = """ "" """
		val lintResult = TestUtil.lint(sourceCode, true)
		val stringLiteralType = lintResult.find<StringLiteral>()?.type
		assertIs<ObjectType>(stringLiteralType)
		assertNotNull(stringLiteralType.definition)
	}

	@Test
	fun `loads number literal type`() {
		val sourceCode = "0"
		val lintResult = TestUtil.lint(sourceCode, true)
		val numberLiteralType = lintResult.find<NumberLiteral>()?.type
		assertIs<ObjectType>(numberLiteralType)
		assertNotNull(numberLiteralType.definition)
	}

	@Test
	fun `loads boolean literal type`() {
		val sourceCode = "yes"
		val lintResult = TestUtil.lint(sourceCode, true)
		val booleanLiteralType = lintResult.find<BooleanLiteral>()?.type
		assertIs<ObjectType>(booleanLiteralType)
		assertNotNull(booleanLiteralType.definition)
	}

	@Test
	fun `loads null literal type`() {
		val sourceCode = "null"
		val lintResult = TestUtil.lint(sourceCode, true)
		val nullLiteralType = lintResult.find<NullLiteral>()?.type
		assertIs<ObjectType>(nullLiteralType)
		assertNotNull(nullLiteralType.definition)
	}

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
				class Bird {}
				var bird: Bird
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<VariableValueDeclaration> { declaration -> declaration.name == "bird" }
		assertNotNull((declaration?.type as? ObjectType)?.definition)
	}

	@Test
	fun `resolves object types`() {
		val sourceCode =
			"""
				object BirdFeeder {}
				var birdFeeder: BirdFeeder
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<VariableValueDeclaration> { declaration -> declaration.name == "birdFeeder" }
		assertEquals("BirdFeeder", (declaration?.type as? ObjectType)?.definition?.name)
	}

	@Test
	fun `resolves enum types`() {
		val sourceCode =
			"""
				enum BirdType {}
				var birdType: BirdType
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<VariableValueDeclaration> { declaration -> declaration.name == "birdType" }
		assertEquals("BirdType", (declaration?.type as? ObjectType)?.definition?.name)
	}

	@Test
	fun `resolves trait types`() {
		val sourceCode =
			"""
				trait Feedable {}
				var feedable: Feedable
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<VariableValueDeclaration> { declaration -> declaration.name == "feedable" }
		assertEquals("Feedable", (declaration?.type as? ObjectType)?.definition?.name)
	}

	@Test
	fun `resolves type alias types`() {
		val sourceCode =
			"""
				alias EventHandler = =>|
				var eventHandler: EventHandler
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<VariableValueDeclaration> { declaration -> declaration.name == "eventHandler" }
		assertEquals("EventHandler", (declaration?.type as? ObjectType)?.definition?.name)
	}

	@Test
	fun `resolves generic types in definitions`() {
		val sourceCode =
			"""
				class List {
					containing Element
					init
					to add(element: Element) {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val declaration = lintResult.find<VariableValueDeclaration> { declaration -> declaration.name == "element" }
		assertEquals("Element", (declaration?.type as? ObjectType)?.definition?.name)
	}

	@Test
	fun `resolves generic types in function parameters`() {
		val sourceCode =
			"""
				class List {
					containing Element
					init
					to add(element: Element) {}
				}
				class Country {}
				object Germany: Country {}
				val list = <Country>List()
				list.add(Germany)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val memberAccess = lintResult.find<MemberAccess> { memberAccess -> memberAccess.member.name == "add" }
		assertEquals("(Country) =>|", memberAccess?.type.toString())
	}
}
