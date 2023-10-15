package components.semantic_model.declarations

import logger.Severity
import logger.issues.declaration.AbstractMemberInNonAbstractTypeDefinition
import logger.issues.declaration.MissingImplementations
import logger.issues.modifiers.*
import org.junit.jupiter.api.Test
import util.TestUtil

internal class Properties {

	@Test
	fun `allows for properties to be overridden`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Nutritional class {
					val nutritionScore: Number
				}
				Food class: Nutritional {
					containing N: Number
					overriding val nutritionScore: N
				}
				Vegetable class: <N>Food {
					containing N: Number
				}
				Potato class: <Float>Vegetable {
					overriding val nutritionScore: Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingOverridingKeyword>()
		lintResult.assertIssueNotDetected<OverriddenSuperMissing>()
		lintResult.assertIssueNotDetected<OverridingPropertyTypeNotAssignable>()
		lintResult.assertIssueNotDetected<OverridingPropertyTypeMismatch>()
		lintResult.assertIssueNotDetected<VariablePropertyOverriddenByValue>()
	}

	@Test
	fun `detects missing overriding keyword on properties`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					val nutritionScore: Number
				}
				Vegetable class: Food
				Potato class: Vegetable {
					val nutritionScore: Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingOverridingKeyword>(
			"Property 'nutritionScore: Float' is missing the 'overriding' keyword.")
	}

	@Test
	fun `detects overriding keyword being used without super property`() {
		val sourceCode =
			"""
				Room class {
					overriding val isClean = yes
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverriddenSuperMissing>(
			"'overriding' keyword is used, but the property doesn't have a super property.")
	}

	@Test
	fun `allows overriding generic value property`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Rectangle class {
					containing N: Number
					val sideLength: N
				}
				FloatRectangle class: <Float>Rectangle {
					overriding val sideLength: Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OverridingPropertyTypeNotAssignable>()
	}

	@Test
	fun `detects overriding value property with incompatible type`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					val nutritionScore: Number
				}
				Potato class: Food {
					overriding val nutritionScore: Food
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingPropertyTypeNotAssignable>(
			"Type of overriding property 'Food' is not assignable to the type of the overridden property 'Number'.",
			Severity.ERROR)
	}

	@Test
	fun `detects overriding variable property with incompatible type`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					var nutritionScore: Number
				}
				Potato class: Food {
					overriding var nutritionScore: Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingPropertyTypeMismatch>(
			"Type of overriding property 'Float' does not match the type of the overridden property 'Number'.", Severity.ERROR)
	}

	@Test
	fun `detects variable property being overridden by value property`() {
		val sourceCode =
			"""
				Number class
				Food class {
					var nutritionScore: Number
				}
				Potato class: Food {
					overriding val nutritionScore: Number
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<VariablePropertyOverriddenByValue>(
			"Variable super property 'nutritionScore' cannot be overridden by value property.", Severity.ERROR)
	}

	@Test
	fun `allows abstract classes to contain abstract properties`() {
		val sourceCode =
			"""
				Int class
				abstract IntList class {
					abstract val size: Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<AbstractMemberInNonAbstractTypeDefinition>()
	}

	@Test
	fun `disallows non-abstract classes to contain abstract properties`() {
		val sourceCode =
			"""
				Int class
				IntList class {
					abstract val size: Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<AbstractMemberInNonAbstractTypeDefinition>(
			"Abstract member 'size: Int' is not allowed in non-abstract type declaration 'IntList'.", Severity.ERROR)
	}

	@Test
	fun `allows abstract classes to not override abstract properties`() {
		val sourceCode =
			"""
				Int class
				abstract IntList class {
					abstract val size: Int
				}
				abstract VoidIntList class: IntList
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `allows non-abstract classes to not override non-abstract properties`() {
		val sourceCode =
			"""
				Int class
				abstract IntList class {
					val size: Int
				}
				VoidIntList class: IntList
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `allows classes to override abstract properties`() {
		val sourceCode =
			"""
				Int class
				abstract IntList class {
					abstract val size: Int
				}
				VoidIntList class: IntList {
					overriding val size: Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `allows classes to not override abstract properties overridden by a super type`() {
		val sourceCode =
			"""
				Float class
				abstract WaterConsumer class {
					abstract var waterLevel: Float
				}
				abstract TallWaterConsumer class: WaterConsumer
				abstract Plant class {
					abstract var waterLevel: Float
				}
				Tree class: Plant {
					overriding var waterLevel: Float
				}
				TallTree class: Tree & TallWaterConsumer
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `allows abstract classes to override abstract properties of generic super type`() {
		val sourceCode =
			"""
				Int class
				abstract List class {
					containing Element
					abstract val first: Element
				}
				IntList class: <Int>List {
					overriding val first: Int
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `doesn't require generic type definitions to override abstract properties`() {
		val sourceCode =
			"""
				Int class
				abstract IntList class {
					abstract val size: Int
				}
				abstract VoidCollections class {
					containing VoidIntList: IntList
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<MissingImplementations>()
	}

	@Test
	fun `disallows non-abstract subclasses that don't implement inherited abstract operators`() {
		val sourceCode = """
			Int class
			abstract Collection class {
				abstract val size: Int
			}
			abstract List class: Collection {
				abstract val minimumSize: Int
				abstract val maximumSize: Int
			}
			LinkedList class: List {
				overriding val minimumSize: Int
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<MissingImplementations>(
			"""
				Non-abstract type declaration 'LinkedList' does not implement the following inherited members:
				 - Collection
				   - size: Int
				 - List
				   - maximumSize: Int
			""".trimIndent(), Severity.ERROR)
	}

	@Test
	fun `allows properties to overwrite properties`() {
		val sourceCode = """
			Computer class {
				val result = 0
			}
			ClassicalComputer class: Computer {
				overriding val result = 1
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueNotDetected<OverridingPropertyTypeMismatch>()
	}

	@Test
	fun `disallows properties to overwrite computed properties`() {
		val sourceCode = """
			Computer class {
				computed result: Int gets 1
			}
			ClassicalComputer class: Computer {
				overriding val result = 0
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingMemberKindMismatch>(
			"'result' property cannot override 'result' computed property.", Severity.ERROR)
	}

	@Test
	fun `disallows properties to overwrite functions`() {
		val sourceCode = """
			Computer class {
				to result()
			}
			ClassicalComputer class: Computer {
				overriding val result = 1
			}
			""".trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertIssueDetected<OverridingMemberKindMismatch>(
			"'result' property cannot override 'result' function.", Severity.ERROR)
	}
}
