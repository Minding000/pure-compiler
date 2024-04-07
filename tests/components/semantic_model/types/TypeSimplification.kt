package components.semantic_model.types

import components.semantic_model.declarations.LocalVariableDeclaration
import components.semantic_model.operations.FunctionCall
import components.semantic_model.operations.MemberAccess
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals

internal class TypeSimplification {

	@Test
	fun `creates or union of independent classes`() {
		val sourceCode =
			"""
				Cat class
				Human class
				Randomizer object {
					to chooseRandomElementOf(Element; first: Element, second: Element): Element
				}
				Randomizer.chooseRandomElementOf(Cat(), Human())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertEquals("Cat | Human", functionCall?.providedType.toString())
	}

	@Test
	fun `doesn't create or union with duplicate class`() {
		val sourceCode =
			"""
				Cat class
				Randomizer object {
					to chooseRandomElementOf(Element; first: Element, second: Element): Element
				}
				val cat = Cat()
				Randomizer.chooseRandomElementOf(cat, cat)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertEquals("Cat", functionCall?.providedType.toString())
	}

	@Test
	fun `doesn't create or union of super and sub class`() {
		val sourceCode =
			"""
				Animal class
				Cat class: Animal
				Randomizer object {
					to chooseRandomElementOf(Element; first: Element, second: Element): Element
				}
				Randomizer.chooseRandomElementOf(Animal(), Cat())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertEquals("Animal", functionCall?.providedType.toString())
	}

	@Disabled("This requires 'potentially optional' generic types.")
	@Test
	fun `creates optional type`() {
		val sourceCode =
			"""
				Cat class
				Randomizer object {
					to chooseRandomElementOf(Element; first: Element, second: Element): Element
				}
				Randomizer.chooseRandomElementOf(Cat(), null)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertEquals("Cat?", functionCall?.providedType.toString())
	}

	@Test
	fun `merges additional types into existing or union`() {
		val sourceCode =
			"""
				Monkey class
				Horse class
				Cat class
				Randomizer object {
					to chooseRandomElementOf(Element; first: Element, second: Element): Element
				}
				val horseOrCat: Horse | Cat = Horse()
				Randomizer.chooseRandomElementOf(Monkey(), horseOrCat)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertEquals("Cat | Horse | Monkey", functionCall?.providedType.toString())
	}

	@Test
	fun `merges multiple or unions`() {
		val sourceCode =
			"""
				Human class
				Monkey class
				Cat class
				Horse class
				Randomizer object {
					to chooseRandomElementOf(Element; first: Element, second: Element): Element
				}
				val humanOrMonkey: Human | Monkey = Human()
				val horseOrCat: Cat | Horse = Cat()
				Randomizer.chooseRandomElementOf(humanOrMonkey, horseOrCat)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertEquals("Cat | Horse | Human | Monkey", functionCall?.providedType.toString())
	}

	@Test
	fun `creates or union of independent complex types`() {
		val sourceCode =
			"""
				Human class
				Patient class
				Bob object: Human & Patient
				Horse class
				Randomizer object {
					to chooseRandomElementOf(Element; first: Element, second: Element): Element
				}
				val bob: Human & Patient = Bob
				Randomizer.chooseRandomElementOf(bob, Horse())
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertEquals("Horse | (Human & Patient)", functionCall?.providedType.toString())
	}

	@Disabled
	@Test
	fun `simplifies nested unions`() {
		val sourceCode =
			"""
				Human class
				Patient class
				Bob object: Human & Patient
				val bob: Human & Patient = Bob
				Horse class
				Ferdinant object: Horse & Patient
				val ferdinant: Horse & Patient = Ferdinant
				Randomizer object {
					to chooseRandomElementOf(Element; first: Element, second: Element): Element
				}
				Randomizer.chooseRandomElementOf(bob, ferdinant)
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val functionCall = lintResult.find<FunctionCall> { functionCall -> functionCall.function is MemberAccess }
		assertEquals("(Human | Horse) & Patient", functionCall?.providedType.toString())
	}

	@Disabled
	@Test
	fun `simplifies and unions`() {
		val sourceCode =
			"""
				Human class
				Patient class
				Bob object: Human & Patient
				val patient: Patient = Bob
				if patient is Human
					val human = patient
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode, true)
		val valueDeclaration = lintResult.find<LocalVariableDeclaration> { declaration -> declaration.name == "human" }
		assertEquals("Human & Patient", valueDeclaration?.providedType.toString())
	}
}
