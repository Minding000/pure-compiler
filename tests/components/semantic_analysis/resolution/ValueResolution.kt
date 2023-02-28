package components.semantic_analysis.resolution

import components.semantic_analysis.semantic_model.definitions.Parameter
import components.semantic_analysis.semantic_model.definitions.PropertyDeclaration
import components.semantic_analysis.semantic_model.values.VariableValue
import messages.Message
import org.junit.jupiter.api.Test
import util.TestUtil
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

internal class ValueResolution {

	@Test
	fun `emits error for undeclared variables`() {
		val sourceCode =
			"""
				numberOfDogs
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "Value 'numberOfDogs' hasn't been declared yet")
	}

	@Test
	fun `resolves local variables`() {
		val sourceCode =
			"""
				val numberOfCats = 2
				numberOfCats
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "numberOfCats" }
		assertNotNull(variableValue?.definition)
	}

	@Test
	fun `resolves instance members`() {
		val sourceCode =
			"""
				House object {
					val livingAreaInSquareMeters = 120
				}
				House.livingAreaInSquareMeters
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "livingAreaInSquareMeters" }
		assertNotNull(variableValue?.definition)
	}

	@Test
	fun `infers type of property set to property with inferred type`() {
		val sourceCode =
			"""
				Int class
				MyHood object {
					val size = DefaultHouse.livingAreaInSquareMeters
				}
				DefaultHouse object {
					val livingAreaInSquareMeters = Int()
				}
				MyHood.size
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "size" }
		assertEquals("Int", variableValue?.type.toString())
	}

	@Test
	fun `infers type of property parameter set to property with inferred type`() {
		val sourceCode =
			"""
				Int class
				House class {
					var size = DefaultHouse.livingAreaInSquareMeters
					init(size)
				}
				DefaultHouse object {
					val livingAreaInSquareMeters: Int = Int()
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "part of a circular assignment")
		val parameter = lintResult.find<Parameter>()
		assertEquals("Int", parameter?.type.toString())
	}

	@Test
	fun `disallows circular type inference`() {
		val sourceCode =
			"""
				House object {
					val livingAreaInSquareMeters = Flat.size
				}
				Flat object {
					val size = House.livingAreaInSquareMeters
				}
				House.livingAreaInSquareMeters
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"'livingAreaInSquareMeters' has no value, because it's part of a circular assignment")
		lintResult.assertMessageEmitted(Message.Type.ERROR,
			"'size' has no value, because it's part of a circular assignment")
	}

	@Test
	fun `resolves parameters`() {
		val sourceCode =
			"""
				House object {
					to openDoor(speed: Int) {
						speed
					}
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "speed" }
		assertNotNull(variableValue?.definition)
	}

	@Test
	fun `resolves super properties`() {
		val sourceCode =
			"""
				Door class {
					val isOpen = yes
				}
				GlassDoor object: Door
				GlassDoor.isOpen
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val member = variableValue?.type?.interfaceScope?.resolveValue("isOpen")
		assertNotNull(member)
	}

	@Test
	fun `resolves overriding properties`() {
		val sourceCode =
			"""
				Door class {
					val isOpen = yes
				}
				TransparentDoor class: Door
				GlassDoor object: TransparentDoor {
					overriding val isOpen = no
				}
				GlassDoor.isOpen
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		val variableValue = lintResult.find<VariableValue> { variableValue -> variableValue.name == "GlassDoor" }
		val member = variableValue?.type?.interfaceScope?.resolveValue("isOpen")
		assertIs<PropertyDeclaration>(member)
		assertEquals("no", member.value?.source?.getValue())
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
		lintResult.assertMessageEmitted(Message.Type.WARNING, "Missing 'overriding' keyword")
	}

	@Test
	fun `allows for properties to be overridden`() {
		val sourceCode =
			"""
				Number class
				Float class: Number
				Food class {
					val nutritionScore: Number
				}
				Vegetable class: Food
				Potato class: Vegetable {
					overriding val nutritionScore: Float
				}
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.WARNING, "Missing 'overriding' keyword")
		lintResult.assertMessageNotEmitted(Message.Type.WARNING,
			"'overriding' keyword is used, but the property doesn't have a super property")
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
		lintResult.assertMessageEmitted(Message.Type.WARNING,
			"'overriding' keyword is used, but the property doesn't have a super property")
	}

	@Test
	fun `doesn't emit error for calls to callable value`() {
		val sourceCode =
			"""
				Bird object {
					to fly()
				}
				Bird.fly()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not callable")
	}

	@Test
	fun `doesn't emit error for calls to unresolved value`() {
		val sourceCode =
			"""
				Bird object
				Bird.fly()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageNotEmitted(Message.Type.ERROR, "is not callable")
	}

	@Test
	fun `emits error for calls to uncallable value`() {
		val sourceCode =
			"""
				Bird object {
					var age = 0
				}
				Bird.age()
            """.trimIndent()
		val lintResult = TestUtil.lint(sourceCode)
		lintResult.assertMessageEmitted(Message.Type.ERROR, "'Bird.age' is not callable")
	}
}
