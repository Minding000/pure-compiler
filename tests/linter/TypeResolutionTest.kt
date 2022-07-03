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

	@Test
	fun testTypeAlias() {
		val sourceCode =
			"""
				class Event {}
				alias EventHandler = (Event) =>|
				var correct: EventHandler
				var incorrect: EventEmitter
				var original: (Event) =>|
				original = correct
				original = incorrect
            """.trimIndent()
		TestUtil.assertLinterMessageNotEmitted(Message.Type.ERROR, "Type 'EventHandler' hasn't been declared yet", sourceCode)
		TestUtil.assertLinterMessageEmitted(Message.Type.ERROR, "Type 'EventEmitter' hasn't been declared yet", sourceCode)
		TestUtil.assertLinterMessageNotEmitted(Message.Type.ERROR, "Type 'EventHandler' is not assignable to type '(Event) =>|'", sourceCode)
		TestUtil.assertLinterMessageEmitted(Message.Type.ERROR, "Type 'EventEmitter' is not assignable to type '(Event) =>|'", sourceCode)
	}

	@Test
	fun testGenericType() {
		val sourceCode =
			"""
				class List {
					containing Element
					init
					to add(element: Element) {}
				}
				class Country {}
				object Germany: Country {}
				object Banana {}
				val list = <Country>List()
				list.add(Germany)
				list.add(Banana)
            """.trimIndent()
		TestUtil.assertLinterMessageNotEmitted(Message.Type.ERROR, "Function 'add(Germany)' hasn't been declared yet", sourceCode)
		TestUtil.assertLinterMessageEmitted(Message.Type.ERROR, "Function 'add(Banana)' hasn't been declared yet", sourceCode)
	}
}