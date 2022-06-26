package linter

import TestUtil
import linter.messages.Message
import org.junit.jupiter.api.Test

internal class TypeResolutionTest {

	@Test
	fun testCorrectClassAccess() {
		val sourceCode =
			"""
				class Bird {}
				var correct: Bird
				var incorrect: Birt
            """.trimIndent()
		TestUtil.assertLinterMessageNotEmitted(Message.Type.ERROR, "Type 'Bird' hasn't been declared yet", sourceCode)
		TestUtil.assertLinterMessageEmitted(Message.Type.ERROR, "Type 'Birt' hasn't been declared yet", sourceCode)
	}

	@Test
	fun testObjectDeclaration() {
		val sourceCode =
			"""
				object BirdFeeder {}
				var correct: BirdFeeder
				var incorrect: BirdFeed
            """.trimIndent()
		TestUtil.assertLinterMessageNotEmitted(Message.Type.ERROR, "Type 'BirdFeeder' hasn't been declared yet", sourceCode)
		TestUtil.assertLinterMessageEmitted(Message.Type.ERROR, "Type 'BirdFeed' hasn't been declared yet", sourceCode)
	}

	@Test
	fun testEnumDeclaration() {
		val sourceCode =
			"""
				enum BirdType {}
				var correct: BirdType
				var incorrect: BirdTypo
            """.trimIndent()
		TestUtil.assertLinterMessageNotEmitted(Message.Type.ERROR, "Type 'BirdType' hasn't been declared yet", sourceCode)
		TestUtil.assertLinterMessageEmitted(Message.Type.ERROR, "Type 'BirdTypo' hasn't been declared yet", sourceCode)
	}

	@Test
	fun testTraitDeclaration() {
		val sourceCode =
			"""
				trait Feedable {}
				var correct: Feedable
				var incorrect: Feeding
            """.trimIndent()
		TestUtil.assertLinterMessageNotEmitted(Message.Type.ERROR, "Type 'Feedable' hasn't been declared yet", sourceCode)
		TestUtil.assertLinterMessageEmitted(Message.Type.ERROR, "Type 'Feeding' hasn't been declared yet", sourceCode)
	}
}