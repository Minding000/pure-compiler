package components.semantic_model.resolution

import components.semantic_model.declarations.ValueDeclaration
import components.semantic_model.types.ObjectType
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class FileReference {

	@Test
	fun `types from a referenced file are accessible`() {
		val typeProvider =
			"""
				BirdType enum
            """.trimIndent()
		val typeRequester =
			"""
				referencing ${TestUtil.TEST_MODULE_NAME}.TypeProvider
				var birdType: BirdType
            """.trimIndent()
		val lintResult = TestUtil.lint(mapOf(
			"TypeRequester" to typeRequester,
			"TypeProvider" to typeProvider
		))
		val declaration = lintResult.find<ValueDeclaration>("TypeRequester") { declaration -> declaration.name == "birdType" }
		assertNotNull(declaration)
		assertEquals("BirdType", (declaration.providedType as? ObjectType)?.getTypeDeclaration()?.name)
	}

	@Test
	fun `types from a non-referenced file are inaccessible`() {
		val typeProvider =
			"""
				BirdType enum
            """.trimIndent()
		val typeRequester =
			"""
				var birdType: BirdType
            """.trimIndent()
		val lintResult = TestUtil.lint(mapOf(
			"TypeRequester" to typeRequester,
			"TypeProvider" to typeProvider
		))
		val declaration = lintResult.find<ValueDeclaration>("TypeRequester") { declaration -> declaration.name == "birdType" }
		assertNotNull(declaration)
		assertNull((declaration.providedType as? ObjectType)?.getTypeDeclaration())
	}

	@Test
	fun `aliased types from a referenced file are accessible`() {
		val typeProvider =
			"""
				String class
            """.trimIndent()
		val typeRequester =
			"""
				referencing ${TestUtil.TEST_MODULE_NAME}.String {
					String as Text
				}
				var text: Text
            """.trimIndent()
		val lintResult = TestUtil.lint(mapOf(
			"TypeRequester" to typeRequester,
			"String" to typeProvider
		))
		val declaration = lintResult.find<ValueDeclaration>("TypeRequester") { declaration -> declaration.name == "text" }
		assertNotNull(declaration)
		assertEquals("String", (declaration.providedType as? ObjectType)?.getTypeDeclaration()?.name)
	}

	@Test
	fun `original types from a alias-referenced file are inaccessible`() {
		val typeProvider =
			"""
				String class
            """.trimIndent()
		val typeRequester =
			"""
				referencing ${TestUtil.TEST_MODULE_NAME}.String {
					String as Text
				}
				var string: String
            """.trimIndent()
		val lintResult = TestUtil.lint(mapOf(
			"TypeRequester" to typeRequester,
			"String" to typeProvider
		))
		val declaration = lintResult.find<ValueDeclaration>("TypeRequester") { declaration -> declaration.name == "string" }
		assertNotNull(declaration)
		assertNull((declaration.providedType as? ObjectType)?.getTypeDeclaration())
	}
}
