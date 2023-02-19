package components.semantic_analysis

import components.semantic_analysis.semantic_model.types.ObjectType
import components.semantic_analysis.semantic_model.values.LocalVariableDeclaration
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class OverGeneratorLoops {

	@Test
	fun `provides an index if iterator is an IndexIterator`() {
		//TODO resolve List issues:
		// - Code cleanup.
		val sourceCode =
			"""
				referencing Pure
				val listOfWords = <String>List()
				loop over listOfWords as index, word {}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val indexVariableType = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "index" }?.type
		assertIs<ObjectType>(indexVariableType)
		assertTrue(Linter.SpecialType.INTEGER.matches(indexVariableType))
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
		val keyVariableType = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "word" }?.type
		assertIs<ObjectType>(keyVariableType)
		assertTrue(Linter.SpecialType.STRING.matches(keyVariableType))
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
		val valueVariableType = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "word" }?.type
		assertIs<ObjectType>(valueVariableType)
		assertTrue(Linter.SpecialType.STRING.matches(valueVariableType))
	}

	@Test
	fun `provides index and value if iterable is of PluralType`() {
		val sourceCode =
			"""
				referencing Pure
				type MultipleStrings = ...String
				val listOfWords: MultipleStrings? = null
				if(!listOfWords?) {
					loop over listOfWords as index, word {}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val indexVariableType = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "index" }?.type
		assertIs<ObjectType>(indexVariableType)
		assertTrue(Linter.SpecialType.INTEGER.matches(indexVariableType))
		val valueVariableType = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "word" }?.type
		assertIs<ObjectType>(valueVariableType)
		assertTrue(Linter.SpecialType.STRING.matches(valueVariableType))
	}
}
