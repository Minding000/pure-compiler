package components.semantic_model

import components.semantic_model.context.SpecialType
import components.semantic_model.declarations.LocalVariableDeclaration
import components.semantic_model.types.ObjectType
import logger.issues.declaration.DeclarationMissingTypeOrValue
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class OverGeneratorLoops {

	@Test
	fun `provides an index if iterator is an IndexIterator`() {
		val sourceCode =
			"""
				referencing Pure
				val listOfWords = <String>List()
				loop over listOfWords as index, word {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueNotDetected<DeclarationMissingTypeOrValue>()
		val indexVariableType = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "index" }?.providedType
		assertIs<ObjectType>(indexVariableType)
		assertTrue(SpecialType.INTEGER.matches(indexVariableType))
	}

	@Test
	fun `provides a key if iterator is a KeyIterator`() {
		val sourceCode =
			"""
				referencing Pure
				val mapFromWordToScore = <String, Float>Map()
				loop over mapFromWordToScore as word, score {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueNotDetected<DeclarationMissingTypeOrValue>()
		val keyVariableType = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "word" }?.providedType
		assertIs<ObjectType>(keyVariableType)
		assertTrue(SpecialType.STRING.matches(keyVariableType))
	}

	@Test
	fun `provides a value if iterator is a ValueIterator`() {
		val sourceCode =
			"""
				referencing Pure
				val listOfWords = <String>List()
				loop over listOfWords as word {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueNotDetected<DeclarationMissingTypeOrValue>()
		val valueVariableType = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "word" }?.providedType
		assertIs<ObjectType>(valueVariableType)
		assertTrue(SpecialType.STRING.matches(valueVariableType))
	}

	@Test
	fun `provides index and value if iterable is of PluralType`() {
		val sourceCode =
			"""
				referencing Pure
				val listOfWords: ...String
				loop over listOfWords as index, word {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		lintResult.assertIssueNotDetected<DeclarationMissingTypeOrValue>()
		val indexVariableType = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "index" }?.providedType
		assertIs<ObjectType>(indexVariableType)
		assertTrue(SpecialType.INTEGER.matches(indexVariableType))
		val valueVariableType = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "word" }?.providedType
		assertIs<ObjectType>(valueVariableType)
		assertTrue(SpecialType.STRING.matches(valueVariableType))
	}
}
